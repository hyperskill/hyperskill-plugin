package com.jetbrains.edu.learning

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project

class CourseIgnoreDocumentListener(private val project: Project) : EduDocumentListenerBase(project) {

  override fun documentChanged(event: DocumentEvent) {
    if (!event.isInProjectContent()) return
    val file = fileDocumentManager.getFile(event.document) ?: return
    if (file.name != EduNames.COURSE_IGNORE) return
  }
}
