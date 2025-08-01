package org.hyperskill.academy.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions.ActionDescription
import com.intellij.openapi.util.NlsActions.ActionText
import java.util.function.Supplier
import javax.swing.Icon

abstract class SyncCourseAction(
  private val text: Supplier<@ActionText String>,
  description: Supplier<@ActionDescription String>,
  icon: Icon?
) : DumbAwareAction(text, description, icon) {
  open val loginWidgetText: String get() = text.get()

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return
    synchronizeCourse(project)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!isAvailable(project)) return

    presentation.isEnabledAndVisible = true
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  abstract fun synchronizeCourse(project: Project)

  abstract fun isAvailable(project: Project): Boolean
}