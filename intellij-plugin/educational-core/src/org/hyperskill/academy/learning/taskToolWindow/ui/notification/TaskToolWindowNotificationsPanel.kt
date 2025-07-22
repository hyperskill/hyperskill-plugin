package org.hyperskill.academy.learning.taskToolWindow.ui.notification

import com.intellij.ui.components.panels.NonOpaquePanel
import javax.swing.BoxLayout

class TaskToolWindowNotificationsPanel : NonOpaquePanel() {
  init {
    layout = BoxLayout(this, BoxLayout.Y_AXIS)
  }
}