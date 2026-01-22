package org.hyperskill.academy.learning.courseFormat.ext

import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.coursecreator.StudyItemType.*
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.tasks.Task

val StudyItem.studyItemType: StudyItemType
  get() {
    return when (this) {
      is Task -> TASK_TYPE
      is Lesson -> LESSON_TYPE
      is Section -> SECTION_TYPE
      is Course -> COURSE_TYPE
      else -> error("Unexpected study item class: ${javaClass.simpleName}")
    }
  }

fun StudyItem.getDir(courseDir: VirtualFile): VirtualFile? {
  return when (this) {
    is Course -> courseDir
    is Section -> {
      val sectionParent = (parentOrNull as? StudyItem) ?: return null
      courseDir.findFileByRelativePath(sectionParent.getPathToChildren())?.findChild(name)
    }

    is Lesson -> {
      val lessonParent = (parentOrNull as? StudyItem) ?: return null
      lessonParent.getDir(courseDir)?.findFileByRelativePath(lessonParent.getPathToChildren())?.findChild(name)
    }

    is Task -> (parentOrNull as? Lesson)?.getDir(courseDir)?.let { findDir(it) }
    else -> error("Can't find directory for the item $itemType")
  }
}

fun StudyItem.visitTasks(action: (Task) -> Unit) {
  when (this) {
    is LessonContainer -> visitTasks(action)
    is Lesson -> visitTasks(action)
    is Task -> action(this)
  }
}

fun StudyItem.getPathToChildren(customContentPath: String = ""): String =
  if (this is Course) customContentPath.ifBlank { course.customContentPath } else ""
