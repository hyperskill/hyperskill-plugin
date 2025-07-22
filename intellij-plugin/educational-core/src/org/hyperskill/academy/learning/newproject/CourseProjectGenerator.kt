package org.hyperskill.academy.learning.newproject

import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.TrustedPaths
import com.intellij.idea.ActionsBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.progress.blockingContextToIndicator
import com.intellij.openapi.project.NOTIFICATIONS_SILENT_MODE
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.TaskCancellation
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.ide.progress.withModalProgress
import com.intellij.platform.util.progress.indeterminateStep
import com.intellij.platform.util.progress.progressStep
import com.intellij.platform.util.progress.withRawProgressReporter
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.ui.jcef.JBCefApp
import com.intellij.util.PathUtil
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.messages.Topic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hyperskill.academy.coursecreator.CCUtils.isLocalCourse
import org.hyperskill.academy.coursecreator.ui.CCOpenEducatorHelp
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.courseFileAttributes
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ONLY_IDEA_DIRECTORY
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.createChildFile
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.unpackAdditionalFiles
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillCourseProjectGenerator
import org.hyperskill.academy.learning.submissions.SubmissionSettings
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.VisibleForTesting
import java.io.File
import java.io.IOException
import java.nio.file.Path

/**
 * If you add any new public methods here, please do not forget to add it also to
 * @see HyperskillCourseProjectGenerator
 */
@Suppress("UnstableApiUsage")
abstract class CourseProjectGenerator<S : EduProjectSettings>(
  protected val courseBuilder: EduCourseBuilder<S>,
  protected val course: Course
) {

  @RequiresBlockingContext
  open fun afterProjectGenerated(
    project: Project,
    projectSettings: S,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    // project.isLocalCourse info is stored in PropertiesComponent to keep it after course restart on purpose
    // not to show login widget for local course
    project.isLocalCourse = course.isLocal

    val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
    statusBarWidgetsManager.updateAllWidgets()

    setUpPluginDependencies(project, course)

    if (!SubmissionSettings.getInstance(project).stateOnClose) {
      NavigationUtils.openFirstTask(course, project)
    }

    CourseMetadataProcessor.applyProcessors(project, course, openCourseParams, CourseProjectState.CREATED_PROJECT)

    YamlFormatSynchronizer.saveAll(project)
    YamlFormatSynchronizer.startSynchronization(project)

    if (!course.isStudy && !isHeadlessEnvironment && JBCefApp.isSupported()) {
      CCOpenEducatorHelp.doOpen(project)
    }

    onConfigurationFinished()
  }

  // 'projectSettings' must have S type but due to some reasons:
  //  * We don't know generic parameter of EduPluginConfigurator after it was gotten through extension point mechanism
  //  * Kotlin and Java do type erasure a little differently
  // we use Object instead of S and cast to S when it needed
  @RequiresEdt
  @RequiresBlockingContext
  fun doCreateCourseProject(
    location: String,
    projectSettings: EduProjectSettings,
    openCourseParams: Map<String, String> = emptyMap(),
    initialLessonProducer: () -> Lesson = ::Lesson
  ): Project? {
    return runWithModalProgressBlocking(
      ModalTaskOwner.guess(),
      EduCoreBundle.message("generate.course.progress.title"),
      TaskCancellation.cancellable()
    ) {
      doCreateCourseProjectAsync(location, projectSettings, openCourseParams, initialLessonProducer)
    }
  }

  private suspend fun doCreateCourseProjectAsync(
    location: String,
    projectSettings: EduProjectSettings,
    openCourseParams: Map<String, String>,
    initialLessonProducer: () -> Lesson
  ): Project? {
    @Suppress("UNCHECKED_CAST")
    val castedProjectSettings = projectSettings as S
    applySettings(castedProjectSettings)
    val createdProject = createProject(location, initialLessonProducer) ?: return null

    withContext(Dispatchers.EDT) {
      blockingContext {
        afterProjectGenerated(createdProject, castedProjectSettings, openCourseParams) {
          ApplicationManager.getApplication().messageBus
            .syncPublisher(COURSE_PROJECT_CONFIGURATION)
            .onCourseProjectConfigured(createdProject)
        }
      }
    }
    return createdProject
  }

  /**
   * Applies necessary changes to [course] object before course creation
   */
  protected open fun applySettings(projectSettings: S) {}

  /**
   * Create new project in given location.
   * To create course structure: modules, folders, files, etc. use [CourseProjectGenerator.createCourseStructure]
   *
   * @param locationString location of new project
   *
   * @return project of new course or null if new project can't be created
   */
  private suspend fun createProject(locationString: String, initialLessonProducer: () -> Lesson): Project? {
    val location = File(FileUtil.toSystemDependentName(locationString))
    val projectDirectoryExists = withContext(Dispatchers.IO) {
      location.exists() || location.mkdirs()
    }
    if (!projectDirectoryExists) {
      val message = ActionsBundle.message("action.NewDirectoryProject.cannot.create.dir", location.absolutePath)
      withContext(Dispatchers.EDT) {
        Messages.showErrorDialog(message, ActionsBundle.message("action.NewDirectoryProject.title"))
      }
      return null
    }
    val baseDir = blockingContext {
      LocalFileSystem.getInstance().refreshAndFindFileByIoFile(location)
    }
    if (baseDir == null) {
      LOG.error("Couldn't find '$location' in VFS")
      return null
    }
    blockingContext {
      VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
    }

    RecentProjectsManager.getInstance().lastProjectCreationLocation = PathUtil.toSystemIndependentName(location.parent)

    baseDir.putUserData(COURSE_MODE_TO_CREATE, course.courseMode)

    @Suppress("UnstableApiUsage")
    TrustedPaths.getInstance().setProjectPathTrusted(location.toPath(), true)

    val holder = CourseInfoHolder.fromCourse(course, baseDir)

    // If a course doesn't contain top-level items, let's count course itself as single item for creation.
    // It's a minor workaround to avoid zero end progress during course structure creation.
    val itemsToCreate = maxOf(1, course.items.size)
    // Total progress: item count steps for each top-level item plus one step for project creation itself
    val structureGenerationEndFraction = itemsToCreate.toDouble() / (itemsToCreate + 1)
    progressStep(structureGenerationEndFraction, EduCoreBundle.message("generate.course.structure.progress.text")) {
      createCourseStructure(holder, initialLessonProducer)
    }

    val newProject = progressStep(1.0, EduCoreBundle.message("generate.course.project.progress.text")) {
      openNewCourseProject(location.toPath(), this@CourseProjectGenerator::prepareToOpen)
    } ?: return null

    // A new progress window is needed because here we already have a new project frame,
    // and previous progress is not visible for user anymore
    withModalProgress(
      ModalTaskOwner.project(newProject),
      EduCoreBundle.message("generate.course.progress.title"),
      TaskCancellation.nonCancellable()
    ) {
      indeterminateStep(EduCoreBundle.message("generate.project.unpack.course.project.settings.progress.text")) {
        blockingContext {
          unpackAdditionalFiles(holder, ONLY_IDEA_DIRECTORY)
        }
      }
    }

    // after adding files with settings to .idea directory, almost all settings are synchronized automatically,
    // but the inspection profiles are to be synchronized manually
    ProjectInspectionProfileManager.getInstance(newProject).initializeComponent()

    return newProject
  }

  protected open suspend fun prepareToOpen(project: Project, module: Module) {
    NOTIFICATIONS_SILENT_MODE.set(project, true)
  }

  open fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler()

  open fun setUpProjectLocation(location: Path): Path = location

  private suspend fun openNewCourseProject(
    location: Path,
    prepareToOpenCallback: suspend (Project, Module) -> Unit,
  ): Project? {
    val beforeInitHandler = beforeInitHandler(location)
    val locationToOpen = setUpProjectLocation(location)
    val task = OpenProjectTask(course, prepareToOpenCallback, beforeInitHandler)

    return ProjectManagerEx.getInstanceEx().openProjectAsync(locationToOpen, task)
  }

  /**
   * Creates course structure in directory provided by [holder]
   */
  @VisibleForTesting
  open suspend fun createCourseStructure(holder: CourseInfoHolder<Course>, initialLessonProducer: () -> Lesson = ::Lesson) {
    holder.course.init(false)
    withRawProgressReporter {
      blockingContext {
        blockingContextToIndicator {
          try {
            generateCourseContent(holder, ProgressManager.getInstance().progressIndicator)
          }
          catch (e: IOException) {
            LOG.error("Failed to generate course", e)
          }
        }
      }
    }
  }

  @RequiresBlockingContext
  @Throws(IOException::class)
  private fun generateCourseContent(
    holder: CourseInfoHolder<Course>,
    indicator: ProgressIndicator
  ) {
    measureTimeAndLog("Course content generation") {
      GeneratorUtils.createCourse(holder, indicator)
      createAdditionalFiles(holder)
    }
  }

  private fun addAdditionalFile(eduFile: EduFile) {
    val contains = course.additionalFiles.any { it.name == eduFile.name }

    if (contains) {
      course.additionalFiles = course.additionalFiles.map { if (it.name == eduFile.name) eduFile else it }
    }
    else {
      course.additionalFiles += eduFile
    }
  }

  /**
   * Creates additional files that are not in course object
   * The files are created on FS.
   * Some files that are intended to go into the course archive are also added to the [Course.additionalFiles].
   *
   * The default implementation takes the list of files from [autoCreatedAdditionalFiles], writes them to the FS and augments the list
   * [Course.additionalFiles] with those files that are not excluded by the [EduConfigurator].
   *
   * Consider overriding [autoCreatedAdditionalFiles] instead of this method, and generate the necessary additional files there.
   * Override this method only if it is impossible to generate additional files in-memory, and one needs to write them directly to FS.
   *
   * @param holder contains info about course project like root directory
   *
   * @throws IOException
   */
  @Throws(IOException::class)
  open fun createAdditionalFiles(holder: CourseInfoHolder<Course>) {
    val configurator = holder.course.configurator ?: return

    for (file in autoCreatedAdditionalFiles(holder)) {
      val childFile = createChildFile(holder, holder.courseDir, file.name, file.contents) ?: continue

      val archiveInclusionPolicy = configurator.courseFileAttributes(holder, childFile).archiveInclusionPolicy
      if (archiveInclusionPolicy >= ArchiveInclusionPolicy.AUTHOR_DECISION) {
        addAdditionalFile(file)
      }
    }
  }

  /**
   * Returns the list of additional files that must be added to the project.
   * Examines the FS and [Course.additionalFiles] to check, whether they lack some necessary files.
   */
  open fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> = emptyList()

  class BeforeInitHandler(val callback: (project: Project) -> Unit = { })

  companion object {
    private val LOG: Logger = Logger.getInstance(CourseProjectGenerator::class.java)

    val EDU_PROJECT_CREATED = Key.create<Boolean>("edu.projectCreated")

    val COURSE_MODE_TO_CREATE = Key.create<CourseMode>("edu.courseModeToCreate")

    @Topic.AppLevel
    val COURSE_PROJECT_CONFIGURATION: Topic<CourseProjectConfigurationListener> = createTopic("COURSE_PROJECT_CONFIGURATION")

    fun OpenProjectTask(
      course: Course,
      prepareToOpenCallback: suspend (Project, Module) -> Unit,
      beforeInitHandler: BeforeInitHandler
    ): OpenProjectTask {
      return OpenProjectTask {
        forceOpenInNewFrame = true
        isNewProject = true
        isProjectCreatedWithWizard = true
        runConfigurators = true
        projectName = course.name
        beforeInit = {
          it.putUserData(EDU_PROJECT_CREATED, true)
          beforeInitHandler.callback(it)
        }
        preparedToOpen = {
          StudyTaskManager.getInstance(it.project).course = course
          prepareToOpenCallback(it.project, it)
        }
      }
    }
  }

  fun interface CourseProjectConfigurationListener {
    fun onCourseProjectConfigured(project: Project)
  }
}
