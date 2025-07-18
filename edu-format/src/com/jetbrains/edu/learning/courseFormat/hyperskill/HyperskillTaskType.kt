package com.jetbrains.edu.learning.courseFormat.hyperskill

import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask.Companion.CODE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.PYCHARM_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE

// lexicographical order
@Suppress("unused")
enum class HyperskillTaskType(val type: String, val value: String) {
  ADMIN("admin", "Linux"),
  CHOICE("choice", "Quiz"),
  CODE(CODE_TASK_TYPE, "Programming"),
  DATASET("dataset", "Data"),
  FILL_BLANKS("fill-blanks", "Fill Blanks"),
  FREE_ANSWER("free-answer", "Free Response"),
  MANUAL_SCORE("manual-score", "Manual Score"),
  MATCHING("matching", "Matching"),
  MATH("math", "Math"),
  NUMBER("number", "Number"),
  PARSONS("parsons", "Parsons"),
  PYCHARM(PYCHARM_TASK_TYPE, "Programming"),
  REMOTE_EDU(REMOTE_EDU_TASK_TYPE, "Programming"),
  SORTING("sorting", "Sorting"),
  STRING("string", "Text"),
  TABLE("table", "Table"),
  TEXT("text", "Theory"),
  VIDEO("video", "Video")
}