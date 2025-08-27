package org.hyperskill.academy.rust.learn

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.currentThreadCoroutineScope
import kotlinx.coroutines.launch

private const val COURSE_ID = 16631

@Suppress("HardCodedStringLiteral") // since it's a temporary internal action, there is no need to localize it
class OpenLearnRustCourseAction : AnAction() {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.text = if (RsOpenCourseHelper.isAlreadyStartedCourse(COURSE_ID)) {
      "Open Learn Rust"
    }
    else {
      "Start Learn Rust"
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    currentThreadCoroutineScope().launch {
      RsOpenCourseHelper.openCourse(COURSE_ID)
    }
  }
}
