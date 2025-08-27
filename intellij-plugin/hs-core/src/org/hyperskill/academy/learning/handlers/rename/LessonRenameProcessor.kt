package org.hyperskill.academy.learning.handlers.rename

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.getLesson

class LessonRenameProcessor : EduStudyItemRenameProcessor() {
  override fun getStudyItem(project: Project, course: Course, file: VirtualFile): StudyItem? {
    return file.getLesson(project)
  }
}
