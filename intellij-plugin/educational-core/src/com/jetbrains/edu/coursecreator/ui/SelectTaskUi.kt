package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.TestOnly

private var MOCK: SelectTaskUi? = null

@TestOnly
fun withMockSelectTaskUi(ui: SelectTaskUi, action: () -> Unit) {
  MOCK = ui
  try {
    action()
  }
  finally {
    MOCK = null
  }
}

interface SelectTaskUi {
  fun selectTask(project: Project, course: EduCourse): Task?
}
