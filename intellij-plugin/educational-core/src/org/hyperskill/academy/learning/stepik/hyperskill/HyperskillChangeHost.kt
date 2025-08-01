package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import org.hyperskill.academy.learning.EduExperimentalFeatures
import org.hyperskill.academy.learning.isFeatureEnabled
import org.hyperskill.academy.learning.messages.EduCoreBundle

@Suppress("ComponentNotRegistered")
class HyperskillChangeHost : DumbAwareAction(EduCoreBundle.message("hyperskill.change.host.action.name")), RightAlignedToolbarAction {
  override fun actionPerformed(e: AnActionEvent) {
    val initialValue = PropertiesComponent.getInstance().getValue(HYPERSKILL_URL_PROPERTY, HYPERSKILL_DEFAULT_URL)
    val result = Messages.showInputDialog(
      EduCoreBundle.message("hyperskill.enter.url.description"),
      EduCoreBundle.message("hyperskill.enter.url"),
      null, initialValue, null
    )
    if (result != null) {
      PropertiesComponent.getInstance().setValue(HYPERSKILL_URL_PROPERTY, result)
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)
  }
}