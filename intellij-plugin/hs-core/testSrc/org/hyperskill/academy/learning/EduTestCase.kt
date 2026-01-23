package org.hyperskill.academy.learning

import com.intellij.lang.Language
import com.intellij.openapi.components.ComponentManagerEx
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerKeys
import com.intellij.openapi.fileEditor.impl.PsiAwareFileEditorManagerImpl
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.platform.util.coroutines.childScope
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl
import io.mockk.clearAllMocks
import okhttp3.mockwebserver.MockResponse
import org.apache.http.HttpStatus
import org.hyperskill.academy.coursecreator.settings.CCSettings
import org.hyperskill.academy.coursecreator.yaml.createConfigFiles
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.checker.CheckActionListener
import org.hyperskill.academy.learning.configuration.PlainTextConfigurator
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.configurators.FakeGradleConfigurator
import org.hyperskill.academy.learning.configurators.FakeGradleHyperskillConfigurator
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL
import org.hyperskill.academy.learning.courseFormat.ext.customContentPath
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowFactory
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.yaml.YamlFormatSettings
import org.hyperskill.academy.rules.CustomValuesRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.io.IOException
import kotlin.reflect.KMutableProperty0

@RunWith(JUnit4::class)
abstract class EduTestCase : BasePlatformTestCase() {

  protected open val useDocumentListener: Boolean = true

  @Rule
  @JvmField
  val customValuesRule = CustomValuesRule()

  @Throws(Exception::class)
  override fun setUp() {
    super.setUp()
    // In this method course is set before course files are created so `CCProjectComponent.createYamlConfigFilesIfMissing` is called
    // for course with no files. This flag is checked in this method and it does nothing if the flag is false
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
    registerConfigurator<PlainTextConfigurator>(PlainTextLanguage.INSTANCE, courseType = HYPERSKILL)
    registerConfigurator<PlainTextConfigurator>(PlainTextLanguage.INSTANCE, environment = EduNames.ANDROID)
    registerConfigurator<FakeGradleConfigurator>(FakeGradleBasedLanguage)
    registerConfigurator<FakeGradleHyperskillConfigurator>(FakeGradleBasedLanguage, courseType = HYPERSKILL)

    // Mock tool window provided by default headless implementation of `ToolWindowManager` doesn't keep any state.
    // As a result, it's impossible to write tests which check tool window state.
    // `EduToolWindowHeadlessManager` allows us to course necessary properties in our tests
    project.replaceService(ToolWindowManager::class.java, EduToolWindowHeadlessManager(project), testRootDisposable)

    EduTestServiceStateHelper.restoreState(project)

    CheckActionListener.reset()
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
      override fun courseSet(course: Course) {
        if (useDocumentListener) {
          EduDocumentListener.setGlobalListener(project, testRootDisposable)
        }
        connection.disconnect()
      }
    })

    createCourse()
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, true)
  }

  override fun tearDown() {
    try {
      // Close all open editors to avoid "Editor is already disposed" errors in subsequent tests
      runInEdtAndWait {
        val fileEditorManager = FileEditorManager.getInstance(project)
        for (file in fileEditorManager.openFiles) {
          fileEditorManager.closeFile(file)
        }
      }
      EduTestServiceStateHelper.cleanUpState(project)
      // Workaround to minimize how test cases affect each other with leaking mocks
      clearAllMocks()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Throws(IOException::class)
  protected open fun createCourse() {
    val course = HyperskillCourse()
    course.name = "Hyperskill test course"
    course.languageId = PlainTextLanguage.INSTANCE.id
    StudyTaskManager.getInstance(project).course = course
  }

  /**
   * Be aware: this method overrides any selection in editor
   * because [com.intellij.testFramework.fixtures.CodeInsightTestFixture.configureFromExistingVirtualFile] loads selection and caret from markup in text
   */
  protected fun configureByTaskFile(lessonIndex: Int, taskIndex: Int, taskFileName: String) {
    var contentPath = myFixture.project.course.customContentPath
    if (contentPath.isNotEmpty()) {
      contentPath = contentPath.trimEnd { it == '/' } + '/'
    }
    val fileName = "${contentPath}lesson$lessonIndex/task$taskIndex/$taskFileName"
    val file = myFixture.findFileInTempDir(fileName)
    myFixture.configureFromExistingVirtualFile(file)
    FileEditorManager.getInstance(myFixture.project).openFile(file, true)
    TaskToolWindowView.getInstance(myFixture.project).currentTask = myFixture.project.getCurrentTask()
  }

  override fun getTestDataPath(): String {
    return TEST_DATA_ROOT
  }

  fun courseWithFiles(
    name: String = "Test Course",
    courseMode: CourseMode = CourseMode.STUDENT,
    id: Int? = null,
    description: String = "Test Course Description",
    environment: String = "",
    language: Language = PlainTextLanguage.INSTANCE,
    courseProducer: () -> Course = ::HyperskillCourse,
    createYamlConfigs: Boolean = false,
    customPath: String = "",
    buildCourse: CourseBuilder.() -> Unit
  ): Course {
    val course = course(name, language, description, environment, courseMode, courseProducer, buildCourse).apply {
      customContentPath = customPath

      initializeCourse(project, course)
      createCourseFiles(project)
    }
    if (id != null) {
      course.id = id
    }
    if (createYamlConfigs) {
      runInEdtAndWait {
        createConfigFiles(project)
      }
    }

    SubmissionsManager.getInstance(project).course = course
    // Cache test files for framework lessons to enable correct test file recreation during navigation
    cacheFrameworkLessonTestFiles(course)
    return course
  }

  /**
   * Cache test files for all tasks in framework lessons to enable proper test file recreation during navigation.
   * This is normally done when loading task data from API, but tests don't make API calls.
   * Call this after course setup is complete (including any modifications to the course structure).
   */
  internal fun cacheFrameworkLessonTestFiles(course: Course) {
    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
    for (lesson in course.lessons.filterIsInstance<FrameworkLesson>()) {
      for (task in lesson.taskList) {
        frameworkLessonManager.storeOriginalTestFiles(task)
      }
    }
  }

  protected fun getCourse(): Course = StudyTaskManager.getInstance(project).course!!

  protected fun findTask(lessonIndex: Int, taskIndex: Int): Task = findLesson(lessonIndex).taskList[taskIndex]

  protected fun findTask(
    sectionIndex: Int,
    lessonIndex: Int,
    taskIndex: Int
  ): Task = getCourse().sections[sectionIndex].lessons[lessonIndex].taskList[taskIndex]

  protected fun findLesson(lessonIndex: Int): Lesson = getCourse().lessons[lessonIndex]

  protected fun getLessons(lessonContainer: LessonContainer? = null): List<Lesson> {
    val container = lessonContainer ?: getCourse()
    return container.lessons
  }

  protected fun findFileInTask(lessonIndex: Int, taskIndex: Int, taskFilePath: String): VirtualFile {
    return findTask(lessonIndex, taskIndex).getTaskFile(taskFilePath)?.getVirtualFile(project)!!
  }

  protected fun findFile(path: String): VirtualFile =
    LightPlatformTestCase.getSourceRoot().findFileByRelativePath(path) ?: error("Can't find `$path`")

  protected fun Task.openTaskFileInEditor(taskFilePath: String) {
    val taskFile = getTaskFile(taskFilePath) ?: error("Can't find task file `$taskFilePath` in `$name`")
    val file = taskFile.getVirtualFile(project) ?: error("Can't find virtual file for `${taskFile.name}` task")
    myFixture.openFileInEditor(file)
    TaskToolWindowView.getInstance(myFixture.project).currentTask = this
  }

  protected fun Task.createTaskFileAndOpenInEditor(taskFilePath: String, text: String = "") {
    val taskDir = getDir(project.courseDir) ?: error("Can't find task dir")
    val file =
      GeneratorUtils.createTextChildFile(project, taskDir, taskFilePath, text) ?: error("Failed to create `$taskFilePath` in $taskDir")
    myFixture.openFileInEditor(file)
  }

  protected inline fun withVirtualFileListener(course: Course, action: () -> Unit) {
    withVirtualFileListener(project, course, testRootDisposable, action)
  }

  protected fun getTestFile(fileName: String) = testDataPath + fileName

  protected fun mockResponse(fileName: String, responseCode: Int = HttpStatus.SC_OK): MockResponse =
    MockResponseFactory.fromFile(getTestFile(fileName), responseCode)

  protected fun checkFileTree(block: FileTreeBuilder.() -> Unit) {
    fileTree(block).assertEquals(LightPlatformTestCase.getSourceRoot(), myFixture)
  }

  companion object {
    const val TEST_DATA_ROOT = "testData"
  }

  protected fun <T, V> withSettingsValue(property: KMutableProperty0<V>, value: V, action: () -> T): T {
    val oldValue = property.get()
    property.set(value)
    return try {
      action()
    }
    finally {
      property.set(oldValue)
    }
  }

  protected fun <T> withDefaultHtmlTaskDescription(action: () -> T): T {
    return withSettingsValue(CCSettings.getInstance()::useHtmlAsDefaultTaskFormat, true, action)
  }

  /**
   * Manually register a tool window in [ToolWindowHeadlessManagerImpl] by its id.
   *
   * In tests, tool windows are not registered by default.
   * So if you need any tool window in `ToolWindowManager` in tests,
   * you have to register it manually.
   */
  protected fun registerToolWindow(id: String) {
    val toolWindowManager = ToolWindowManager.getInstance(project) as ToolWindowHeadlessManagerImpl
    if (toolWindowManager.getToolWindow(id) == null) {
      for (bean in collectToolWindowExtensions()) {
        if (bean.id == id) {
          toolWindowManager.doRegisterToolWindow(bean.id)
          Disposer.register(testRootDisposable) {
            toolWindowManager.unregisterToolWindow(bean.id)
          }
        }
      }
    }
  }

  protected fun registerTaskDescriptionToolWindow() {
    registerToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW)
  }

  /**
   * Temporarily replace test implementation of `FileEditorManager` with `PsiAwareFileEditorManagerImpl`
   * which is async and used in production
   *
   * @see com.intellij.testFramework.executeSomeCoroutineTasksAndDispatchAllInvocationEvents
   */
  protected fun setProductionFileEditorManager() {
    project.putUserData(FileEditorManagerKeys.ALLOW_IN_LIGHT_PROJECT, true)
    Disposer.register(testRootDisposable) { project.putUserData(FileEditorManagerKeys.ALLOW_IN_LIGHT_PROJECT, null) }

    project.replaceService(
      FileEditorManager::class.java,
      PsiAwareFileEditorManagerImpl(project, (project as ComponentManagerEx).getCoroutineScope().childScope(name)),
      testRootDisposable
    )
  }
}
