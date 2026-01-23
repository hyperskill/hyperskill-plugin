package org.hyperskill.academy.learning.actions

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.ui.RUN_CONFIGURATION_KEY
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.EduNames.RUN_CONFIGURATION_DIR
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.actions.RunTaskAction.Companion.RUN_CONFIGURATION_FILE_NAME
import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager

class AssignRunConfigurationToTask : AnAction(), DumbAware {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    return
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    val selectedConfiguration = e.getConfigurationFromActionContext(project) ?: return

    //If the task is already assigned to some configuration, unassign it.
    //Otherwise the runner.run.xml will have both the old and the new configuration.
    val existingConfiguration = CheckUtils.getCustomRunConfigurationForRunner(project, task)
    if (existingConfiguration != null) {
      existingConfiguration.storeInLocalWorkspace()
      existingConfiguration.name = "Unassigned ${existingConfiguration.name}"
      forceSaveRunConfigurationInFile(project, existingConfiguration)
    }

    val taskDir = project.courseDir.findFileByRelativePath(task.pathInCourse) ?: return

    selectedConfiguration.name = "Run task: ${task.name} (${task.parent.name})"
    (selectedConfiguration.configuration as? LocatableConfigurationBase<*>)?.setNameChangedByUser(true)
    selectedConfiguration.storeInArbitraryFileInProject("${taskDir.path}/$RUN_CONFIGURATION_DIR/$RUN_CONFIGURATION_FILE_NAME")

    forceSaveRunConfigurationInFile(project, selectedConfiguration)

    EduNotificationManager.showInfoNotification(
      project = project,
      title = EduCoreBundle.message("actions.run.task.configuration.assigned.title"),
      content = EduCoreBundle.message("actions.run.task.configuration.assigned.message", task.name, selectedConfiguration.name)
    )
  }

  /**
   * Returns a configuration, for which the contextual menu is open,
   * or a currently selected action, if the action is called from somewhere else.
   */
  private fun AnActionEvent.getConfigurationFromActionContext(project: Project): RunnerAndConfigurationSettings? {
    val contextualConfiguration = getData(RUN_CONFIGURATION_KEY)
    if (contextualConfiguration != null) {
      return contextualConfiguration
    }

    return RunManager.getInstance(project).selectedConfiguration
  }

  private fun forceSaveRunConfigurationInFile(
    project: Project, selectedConfiguration: RunnerAndConfigurationSettings
  ) {
    /**
     * Although the configuration is already tracked by the RunManager,
     * we add it to update IDE internal data structures that store the list of configurations.
     * Otherwise, IDE decides that the configuration is not changed and there is no need to update its storage.
     */
    RunManager.getInstance(project).addConfiguration(selectedConfiguration)
    SaveAndSyncHandler.getInstance().scheduleProjectSave(project)
  }

  companion object {
    const val ACTION_ID = "HyperskillEducational.AssignRunConfigurationToTask"
  }
}