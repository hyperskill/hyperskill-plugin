package org.hyperskill.academy.learning.newproject

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import org.hyperskill.academy.learning.RemoteEnvHelper

@Suppress("ComponentNotRegistered") // educational-core.xml
class LearnAndTeachFileMenuGroup : DefaultActionGroup(), DumbAware {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = !RemoteEnvHelper.isRemoteDevServer()
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}