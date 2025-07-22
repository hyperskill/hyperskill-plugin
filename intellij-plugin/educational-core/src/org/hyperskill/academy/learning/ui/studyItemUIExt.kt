package org.hyperskill.academy.learning.ui

import com.intellij.openapi.util.NlsActions
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.coursecreator.presentableName
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.CodeTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.courseFormat.tasks.UnsupportedTask
import org.hyperskill.academy.learning.messages.EduCoreBundle


fun Task.getUIName(): String = if (course is HyperskillCourse) {
  if (this is CodeTask) EduCoreBundle.message("item.task.challenge") else EduCoreBundle.message("item.task.stage")
}
else {
  StudyItemType.TASK_TYPE.presentableName
}

@NlsActions.ActionText
fun Task.getUICheckLabel(): String {
  val defaultMessage = EduCoreBundle.message("action.HyperskillEducational.Check.text")

  return when (this) {
    is TheoryTask -> EduCoreBundle.message("action.check.run.text")
    is UnsupportedTask -> EduCoreBundle.message("hyperskill.unsupported.check.task")
    else -> defaultMessage
  }
}