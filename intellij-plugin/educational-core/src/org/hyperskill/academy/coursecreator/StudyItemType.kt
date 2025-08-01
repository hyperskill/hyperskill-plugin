package org.hyperskill.academy.coursecreator

import com.intellij.openapi.keymap.KeymapUtil
import org.hyperskill.academy.coursecreator.StudyItemType.*
import org.hyperskill.academy.learning.messages.EduCoreBundle.message
import org.jetbrains.annotations.Nls
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

enum class StudyItemType {
  COURSE_TYPE,
  SECTION_TYPE,
  LESSON_TYPE,
  TASK_TYPE;
}

val StudyItemType.presentableName: String
  @Nls
  get() = when (this) {
    COURSE_TYPE -> message("item.course")
    SECTION_TYPE -> message("item.section")
    LESSON_TYPE -> message("item.lesson")
    TASK_TYPE -> message("item.task")
  }


val StudyItemType.presentableTitleName: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE_TYPE -> message("item.course.title")
    SECTION_TYPE -> message("item.section.title")
    LESSON_TYPE -> message("item.lesson.title")
    TASK_TYPE -> message("item.task.title")
  }

val StudyItemType.newItemTitleMessage: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE_TYPE -> message("item.new.course.title")
    SECTION_TYPE -> message("item.new.section.title")
    LESSON_TYPE -> message("item.new.lesson.title")
    TASK_TYPE -> message("item.new.task.title")
  }

val StudyItemType.selectItemTypeMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() = when (this) {
    COURSE_TYPE -> message("item.select.type.course")
    SECTION_TYPE -> message("item.select.type.section")
    LESSON_TYPE -> message("item.select.type.lesson")
    TASK_TYPE -> message("item.select.type.task")
  }

val StudyItemType.pressEnterToCreateItemMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() {
    val enter = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    return when (this) {
      COURSE_TYPE -> message("item.hint.press.enter.to.create.course", enter)
      SECTION_TYPE -> message("item.hint.press.enter.to.create.section", enter)
      LESSON_TYPE -> message("item.hint.press.enter.to.create.lesson", enter)
      TASK_TYPE -> message("item.hint.press.enter.to.create.task", enter)
    }
  }

val StudyItemType.moveItemMessage: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE_TYPE -> message("dialog.title.move.course")
    SECTION_TYPE -> message("dialog.title.move.section")
    LESSON_TYPE -> message("dialog.title.move.lesson")
    TASK_TYPE -> message("dialog.title.move.task")
  }

