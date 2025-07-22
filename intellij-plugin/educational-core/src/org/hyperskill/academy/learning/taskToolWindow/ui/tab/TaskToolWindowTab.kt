package org.hyperskill.academy.learning.taskToolWindow.ui.tab

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBEmptyBorder
import org.hyperskill.academy.learning.EduSettings
import org.hyperskill.academy.learning.JavaUILibrary
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import java.awt.BorderLayout
import javax.swing.JPanel

abstract class TaskToolWindowTab(val project: Project) : JPanel(BorderLayout()), Disposable {
  protected open val uiMode: JavaUILibrary = EduSettings.getInstance().javaUiLibrary

  init {
    border = JBEmptyBorder(0)
  }

  abstract fun update(task: Task)

  override fun dispose() {}
}
