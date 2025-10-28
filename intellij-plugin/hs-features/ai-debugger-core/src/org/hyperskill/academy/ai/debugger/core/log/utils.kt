package org.hyperskill.academy.ai.debugger.core.log

import  org.hyperskill.academy.learning.courseFormat.tasks.Task

fun AIDebuggerLogEntry.logInfo() {
  AIDebuggerLoggerFactory.getInstance().info(this.toString())
}

fun AIDebuggerLogEntry.logError() {
  AIDebuggerLoggerFactory.getInstance().error(this.toString())
}

fun Task.toTaskData(): TaskData = TaskData(course.id, lesson.id, id)
