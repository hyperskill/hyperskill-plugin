package org.hyperskill.academy.learning

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.ExactFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.backend.observation.trackActivity
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hyperskill.academy.coursecreator.courseignore.CourseIgnoreFileType
import org.hyperskill.academy.coursecreator.framework.SyncChangesStateManager
import org.hyperskill.academy.coursecreator.handlers.CCVirtualFileListener
import org.hyperskill.academy.learning.EduNames.COURSE_IGNORE
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.EduUtilsKt.isNewlyCreated
import org.hyperskill.academy.learning.EduUtilsKt.isStudentProject
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.handlers.UserCreatedFileListener
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.navigation.NavigationUtils.setHighlightLevelForFilesInTask
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.projectView.CourseViewPane
import org.hyperskill.academy.learning.submissions.SubmissionSettings
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.VisibleForTesting

class EduProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) = project.trackActivity(EduCourseConfigurationActivityKey) {
    if (!project.isEduProject()) return@trackActivity

    val manager = StudyTaskManager.getInstance(project)
    val connection = ApplicationManager.getApplication().messageBus.connect(manager)
    if (!isUnitTestMode) {
      val vfsListener = if (project.isStudentProject()) UserCreatedFileListener(project) else CCVirtualFileListener(project, manager)
      connection.subscribe(VirtualFileManager.VFS_CHANGES, vfsListener)

      EduDocumentListener.setGlobalListener(project, manager)
    }

    ensureCourseIgnoreHasNoCustomAssociation()

    // Not sure we want to wait
    project.waitForSmartMode()

    val course = manager.course
    if (course == null) {
      LOG.warn("Opened project is with null course")
      return@trackActivity
    }

    withContext(Dispatchers.EDT) {
      val fileEditorManager = FileEditorManager.getInstance(project)
      if (!fileEditorManager.hasOpenFiles() && !SubmissionSettings.getInstance(project).stateOnClose) {
        NavigationUtils.openFirstTask(course, project)
      }
    }
    selectProjectView(project, true)

    withContext(Dispatchers.EDT) {
      blockingContext {
        migrateYaml(project, course)
        setupProject(project, course)
      }
      val coursesStorage = CoursesStorage.getInstance()
      val location = project.basePath
      if (!coursesStorage.hasCourse(course) && location != null) {
        coursesStorage.addCourse(course, location)
      }
    }

    SyncChangesStateManager.getInstance(project).updateSyncChangesState(course)

    writeAction {
      if (project.isStudentProject()) {
        course.visitTasks {
          setHighlightLevelForFilesInTask(it, project)
        }
      }
    }
  }

  @VisibleForTesting
  @RequiresBlockingContext
  fun migrateYaml(project: Project, course: Course) {
    migrateCanCheckLocallyYaml(project, course)
    YamlFormatSynchronizer.saveAll(project)
  }

  private fun migrateCanCheckLocallyYaml(project: Project, course: Course) {
    val propertyComponent = PropertiesComponent.getInstance(project)
    if (propertyComponent.getBoolean(YAML_MIGRATED)) return
    propertyComponent.setValue(YAML_MIGRATED, true)
    if (course !is HyperskillCourse) return
  }

  private suspend fun ensureCourseIgnoreHasNoCustomAssociation() {
    writeAction {
      FileTypeManager.getInstance().associate(CourseIgnoreFileType, ExactFileNameMatcher(COURSE_IGNORE))
    }
  }

  // In general, it's hack to select proper Project View pane for course projects
  // Should be replaced with proper API
  private fun selectProjectView(project: Project, retry: Boolean) {
    ToolWindowManager.getInstance(project).invokeLater {
      val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.PROJECT_VIEW)
      // Since 2020.1 project view tool window can be uninitialized here yet
      if (toolWindow == null) {
        if (retry) {
          selectProjectView(project, false)
        }
        else {
          LOG.warn("Failed to show Course View because Project View is not initialized yet")
        }
        return@invokeLater
      }
      val projectView = ProjectView.getInstance(project)
      if (projectView != null) {
        val selectedViewId = ProjectView.getInstance(project).currentViewId
        if (CourseViewPane.ID != selectedViewId) {
          projectView.changeView(CourseViewPane.ID)
        }
      }
      else {
        LOG.warn("Failed to select Project View")
      }
      toolWindow.show()
    }
  }

  @RequiresEdt
  @RequiresBlockingContext
  private fun setupProject(project: Project, course: Course) {
    val configurator = course.configurator
    if (configurator == null) {
      LOG.warn("Failed to refresh gradle project: configurator for `${course.languageId}` is null")
      return
    }

    if (!isUnitTestMode && project.isNewlyCreated()) {
      configurator.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    }

    // Android Studio creates `gradlew` not via VFS, so we have to refresh project dir
    runInBackground(project, EduCoreBundle.message("refresh.course.project.directory"), false) {
      VfsUtil.markDirtyAndRefresh(false, true, true, project.courseDir)
    }
  }

  companion object {
    private val LOG: Logger = logger<EduProjectActivity>()

    private const val YAML_MIGRATED = "Hyperskill.Yaml.Migrate"
  }
}
