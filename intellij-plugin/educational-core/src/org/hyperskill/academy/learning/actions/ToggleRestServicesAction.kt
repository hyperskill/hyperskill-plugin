package org.hyperskill.academy.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareToggleAction
import org.hyperskill.academy.learning.authUtils.OAuthRestService
import org.hyperskill.academy.learning.messages.EduCoreBundle

class ToggleRestServicesAction : DumbAwareToggleAction(EduCoreBundle.lazyMessage("action.toggle.rest.services.title")) {

  override fun isSelected(e: AnActionEvent): Boolean = OAuthRestService.isRestServicesEnabled

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    OAuthRestService.isRestServicesEnabled = state
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
