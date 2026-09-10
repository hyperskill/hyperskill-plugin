package org.hyperskill.academy.learning.courseFormat.ext

import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.coursecreator.StudyItemType.*
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.findFileByRelativePathOrSelf

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
      courseDir.findFileByRelativePathOrSelf(sectionParent.getPathToChildren())?.findChild(name)
    }

    is Lesson -> {
      val lessonParent = (parentOrNull as? StudyItem) ?: return null
      lessonParent.getDir(courseDir)?.findFileByRelativePathOrSelf(lessonParent.getPathToChildren())?.findChild(name)
    }

    is Task -> (parentOrNull as? Lesson)
        ?.getDir(courseDir)
        ?.findChild(targetDirName)
    else -> error("Can't find directory for the item $itemType")
  }
}

/**
 * The course this item belongs to, or `null` if the item is not attached to one.
 *
 * Unlike [StudyItem.course] it doesn't fail on items with a missing parent link, which happens when the course
 * structure couldn't be fully restored from the config files.
 */
val StudyItem.courseOrNull: Course?
  get() {
    var item: StudyItem? = this
    while (item != null) {
      if (item is Course) return item
      item = item.parentOrNull
    }
    return null
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
