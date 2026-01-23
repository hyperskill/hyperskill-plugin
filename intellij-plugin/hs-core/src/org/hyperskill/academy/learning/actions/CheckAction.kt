package org.hyperskill.academy.learning.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.impl.NotificationSettings
import com.intellij.notification.impl.NotificationsConfigurationImpl
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.writeBytes
import com.intellij.psi.PsiDocumentManager
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.EduUtilsKt.showPopup
import org.hyperskill.academy.learning.actions.EduActionUtils.showFakeProgress
import org.hyperskill.academy.learning.actions.EduActionUtils.updateAction
import org.hyperskill.academy.learning.checker.CheckListener
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.details.CheckDetailsView
import org.hyperskill.academy.learning.checker.remote.RemoteTaskCheckerManager.remoteCheckerForTask
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.CheckResult.Companion.failedToCheck
import org.hyperskill.academy.learning.courseFormat.ext.*
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.OutputTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.hyperskill.academy.learning.messages.EduCoreBundle.message
import org.hyperskill.academy.learning.projectView.ProgressUtil.updateCourseProgress
import org.hyperskill.academy.learning.stepik.hyperskill.checker.HyperskillCheckConnector.failedToSubmit
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.taskToolWindow.ui.check.CheckPanel
import org.hyperskill.academy.learning.ui.getUICheckLabel
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.saveItem
import org.jetbrains.annotations.NonNls
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class CheckAction() : ActionWithProgressIcon(), DumbAware {

  constructor(@NlsActions.ActionText checkLabel: String) : this() {
    templatePresentation.text = checkLabel
  }

  init {
    setUpSpinnerPanel(PROCESS_MESSAGE)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (DumbService.isDumb(project)) {
      e.dataContext.showPopup(ActionUtil.getActionUnavailableMessage(message("check.title")))
      return
    }
    CheckDetailsView.getInstance(project).clear()
    FileDocumentManager.getInstance().saveAllDocuments()
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    if (!CheckActionState.getInstance(project).doLock()) {
      e.dataContext.showPopup(message("action.check.already.running"))
      return
    }
    for (listener in CheckListener.EP_NAME.extensionList) {
      listener.beforeCheck(project, task)
    }
    val checkTask = StudyCheckTask(project, task)
    if (isUnitTestMode) {
      // It's hack to make checker tests work properly.
      // `com.intellij.openapi.progress.ProgressManager.run(com.intellij.openapi.progress.Task)` executes task synchronously
      // if the task run in headless environment (e.g. in unit tests).
      // It blocks EDT and any next `ApplicationManager.getApplication().invokeAndWait()` call will hang because of deadlock
      val future = ApplicationManager.getApplication().executeOnPooledThread { ProgressManager.getInstance().run(checkTask) }
      EduActionUtils.waitAndDispatchInvocationEvents(future)
    }
    else {
      ProgressManager.getInstance().run(checkTask)
    }
  }

  override fun update(e: AnActionEvent) {
    if (CheckPanel.ACTION_PLACE == e.place) {
      //action is being added only in valid context, no project in event in this case, so just enable it
      return
    }
    updateAction(e)
    val project = e.project ?: return
    val taskFile = project.selectedTaskFile
    if (taskFile != null) {
      templatePresentation.text = taskFile.task.getUICheckLabel()
    }
    if (e.presentation.isEnabled) {
      e.presentation.isEnabled = !CheckActionState.getInstance(project).isLocked
      return
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private inner class StudyCheckTask(project: Project, private val task: Task) :
    com.intellij.openapi.progress.Task.Backgroundable(project, message("progress.title.checking.solution"), true) {
    private var result: CheckResult? = null
    private val checker: TaskChecker<*>?

    init {
      val configurator = task.course.configurator
      checker = configurator?.taskCheckerProvider?.getTaskChecker(task, project)
    }

    @RequiresEdt
    private fun onStarted(indicator: ProgressIndicator) {
      processStarted()
      ApplicationManager.getApplication().executeOnPooledThread { showFakeProgress(indicator) }
      TaskToolWindowView.getInstance(project).checkStarted(task, false)
    }

    override fun run(indicator: ProgressIndicator) {
      invokeAndWaitIfNeeded {
        onStarted(indicator)
      }
      val start = System.currentTimeMillis()
      val notificationSettings = turnOffTestRunnerNotifications()
      val localCheckResult = localCheck(indicator)
      invokeLater { NotificationsConfigurationImpl.getInstanceImpl().changeSettings(notificationSettings) }
      val end = System.currentTimeMillis()
      LOG.info(String.format("Checking of %s task took %d ms", task.name, end - start))
      if (localCheckResult.status === CheckStatus.Failed) {
        result = localCheckResult
        return
      }
      val remoteChecker = remoteCheckerForTask(project, task)
      result = remoteChecker?.check(project, task, indicator) ?: localCheckResult
    }

    private fun localCheck(indicator: ProgressIndicator): CheckResult {
      if (checker == null) return CheckResult.NO_LOCAL_CHECK
      val taskDir = task.getDir(project.courseDir) ?: return CheckResult.NO_LOCAL_CHECK

      // Recreate test files from task definition before checking
      // This prevents students from cheating by modifying or deleting test files
      recreateTestFiles(taskDir)

      if (task.course !is HyperskillCourse) {
        createTests(invisibleTestFiles)
      }
      return checker.check(indicator)
    }

    private fun recreateTestFiles(taskDir: VirtualFile) {
      // Test files are files provided by the course author (isLearnerCreated = false)
      // that are identified as test files by the language-specific configurator
      // This uses configurator.isTestFile() which checks testDirs and other language-specific rules
      // We recreate them before Check to prevent cheating
      //
      // For framework lessons, we use cached test files from FrameworkLessonManager
      // because task.taskFiles may contain stale data for non-current tasks.
      // The cache is populated from API response when tasks are created/updated.
      val testFiles: Collection<TaskFile> = if (task.lesson is FrameworkLesson) {
        val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
        // ALT-10961: Ensure test files are cached, loading from API if necessary.
        // NEVER fall back to task.taskFiles as it may be corrupted with test content
        // from another stage in framework lessons.
        if (!frameworkLessonManager.ensureTestFilesCached(task)) {
          LOG.warn(
            "Failed to cache test files for framework task '${task.name}'. " +
            "Test files may be missing or incorrect."
          )
        }
        val cachedTestFiles = frameworkLessonManager.getOriginalTestFiles(task)
        if (cachedTestFiles != null) {
          LOG.info("Using ${cachedTestFiles.size} cached test files for framework task '${task.name}'")
          cachedTestFiles
        }
        else {
          LOG.warn("No test files available for framework task '${task.name}' after attempting API load")
          emptyList()
        }
      }
      else {
        task.taskFiles.values.filter { taskFile ->
          !taskFile.isLearnerCreated && taskFile.isTestFile
        }
      }

      invokeAndWaitIfNeeded {
        runWriteAction {
          for (taskFile in testFiles) {
            try {
              GeneratorUtils.createChildFile(
                project.toCourseInfoHolder(),
                taskDir,
                taskFile.name,
                taskFile.contents,
                taskFile.isEditable
              )
            }
            catch (e: Exception) {
              LOG.warn("Failed to recreate test file ${taskFile.name}", e)
            }
          }
        }
      }
    }

    private fun createTests(testFiles: List<TaskFile>) {
      invokeAndWaitIfNeeded {
        testFiles.forEach { file ->
          when (val contents = file.contents) {
            is BinaryContents -> replaceFileBytes(file, contents.bytes)
            is TextualContents -> replaceFileText(file, contents.text)
            is UndeterminedContents -> replaceFileText(file, contents.textualRepresentation)
          }
        }
      }
    }

    private fun replaceFileBytes(file: TaskFile, bytes: ByteArray) {
      CommandProcessor.getInstance().runUndoTransparentAction {
        runWriteAction {
          file.getVirtualFile(project)?.writeBytes(bytes)
        }
      }
    }

    private fun replaceFileText(file: TaskFile, newText: String) {
      val newDocumentText = StringUtil.convertLineSeparators(newText)
      CommandProcessor.getInstance().runUndoTransparentAction {
        runWriteAction {
          val document = file.getDocument(project) ?: return@runWriteAction
          CommandProcessor.getInstance().executeCommand(
            project,
            { document.setText(newDocumentText) },
            message("action.change.test.text"),
            "Edu Actions"
          )
          PsiDocumentManager.getInstance(project).commitAllDocuments()
        }
      }
    }

    private val invisibleTestFiles: List<TaskFile>
      get() = task.taskFiles.values.filter {
        EduUtilsKt.isTestsFile(task, it.name) && !it.isVisible && (task is EduTask || task is OutputTask)
      }

    override fun onSuccess() {
      val checkResult = result
      requireNotNull(checkResult)

      task.status = checkResult.status
      task.feedback = CheckFeedback(Date(), checkResult)
      saveItem(task)

      if (checker != null) {
        if (checkResult.status === CheckStatus.Failed) {
          checker.onTaskFailed()
        }
        else if (checkResult.status === CheckStatus.Solved) {
          checker.onTaskSolved()
        }
      }

      TaskToolWindowView.getInstance(project).checkFinished(task, checkResult)
      project.invokeLater {
        updateCourseProgress(project)
        ProjectView.getInstance(project).refresh()
        for (listener in CheckListener.EP_NAME.extensions) {
          listener.afterCheck(project, task, checkResult)
        }
      }
    }

    override fun onCancel() {
      TaskToolWindowView.getInstance(project).readyToCheck()
    }

    override fun onFinished() {
      checker?.clearState()
      processFinished()
      CheckActionState.getInstance(project).unlock()
    }

    override fun onThrowable(error: Throwable) {
      super.onThrowable(error)
      if (error.message == message("error.failed.to.refresh.tokens")) {
        TaskToolWindowView.getInstance(project)
          .checkFinished(task, failedToSubmit(project, task, message("error.failed.to.refresh.tokens")))
      }
      else {
        TaskToolWindowView.getInstance(project).checkFinished(task, failedToCheck)
      }
    }

    private fun turnOffTestRunnerNotifications(): NotificationSettings {
      val notificationsConfiguration = NotificationsConfigurationImpl.getInstanceImpl()
      val testRunnerSettings = NotificationsConfigurationImpl.getSettings(TEST_RESULTS_DISPLAY_ID)
      notificationsConfiguration.changeSettings(TEST_RESULTS_DISPLAY_ID, NotificationDisplayType.NONE, false, false)
      return testRunnerSettings
    }

    private val TEST_RESULTS_DISPLAY_ID: @NonNls String = "Test Results: Run"
  }

  @Service(Service.Level.PROJECT)
  private class CheckActionState {
    private val isBusy = AtomicBoolean(false)
    fun doLock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    val isLocked: Boolean
      get() = isBusy.get()

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): CheckActionState {
        return project.service()
      }
    }
  }

  companion object {
    private const val PROCESS_MESSAGE = "Check in progress"
    private val LOG = Logger.getInstance(CheckAction::class.java)
  }
}
