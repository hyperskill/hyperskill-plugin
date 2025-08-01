package org.hyperskill.academy.coursecreator.taskDescription

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.DescriptionFormat
import org.intellij.lang.annotations.Language
import org.junit.Test

class TaskDescriptionLiveTemplateTest : EduTestCase() {

  @Test
  fun `test no hint live template in task description file in student mode`() {
    createCourse(CourseMode.STUDENT)

    expandSnippet(
      "lesson/task/task.html", """
      <html>
      hint<caret>
      </html>
    """, """
      <html>
      <hint></hint>
      </html>
    """
    )
  }

  private fun createCourse(courseMode: CourseMode) {
    courseWithFiles(courseMode = courseMode) {
      lesson("lesson") {
        eduTask("task", taskDescriptionFormat = DescriptionFormat.HTML) {
          taskFile("taskFile.html")
        }
      }
    }
  }

  private fun expandSnippet(
    filePath: String,
    @Suppress("SameParameterValue") @Language("HTML") before: String,
    @Language("HTML") after: String
  ) {
    val file = myFixture.findFileInTempDir(filePath)

    runWriteAction {
      VfsUtil.saveText(file, before.trimIndent())
    }
    myFixture.configureFromExistingVirtualFile(file)
    myFixture.performEditorAction(IdeActions.ACTION_EXPAND_LIVE_TEMPLATE_BY_TAB)
    myFixture.availableIntentions
    myFixture.checkResult(after.trimIndent())
  }
}
