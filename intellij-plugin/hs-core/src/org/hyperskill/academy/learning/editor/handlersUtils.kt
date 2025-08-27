package org.hyperskill.academy.learning.editor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.getTaskFile

fun getTaskFile(editor: Editor?): TaskFile? {
  if (editor == null) return null
  val project = editor.project ?: return null
  val openedFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
  return openedFile.getTaskFile(project)
}