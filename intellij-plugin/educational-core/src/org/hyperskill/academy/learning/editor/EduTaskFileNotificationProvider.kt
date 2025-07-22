package org.hyperskill.academy.learning.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import org.hyperskill.academy.learning.getTaskFile
import org.hyperskill.academy.learning.messages.EduCoreBundle
import java.util.function.Function
import javax.swing.JComponent

class EduTaskFileNotificationProvider : EditorNotificationProvider {

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    val taskFile = file.getTaskFile(project) ?: return null
    return Function { fileEditor ->
      (fileEditor as? TextEditor)?.editor?.document?.text ?: return@Function null
      if (!taskFile.isValid()) {
        val panel = EditorNotificationPanel().text(EduCoreBundle.message("error.solution.cannot.be.loaded"))
        panel.createActionLabel(EduCoreBundle.message("action.Educational.RefreshTask.text"), "HyperskillEducational.RefreshTask")
        panel
      }
      else {
        null
      }
    }
  }
}
