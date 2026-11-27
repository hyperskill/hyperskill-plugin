package org.hyperskill.academy.learning.taskToolWindow

import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.EduSettings
import org.hyperskill.academy.learning.JavaUILibrary
import org.hyperskill.academy.learning.NotificationsTestBase
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTaskType
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.ui.quiz.QuizFormNotificationProvider
import org.junit.Test

class QuizFormNotificationProviderTest : NotificationsTestBase() {
  private lateinit var initialUiLibrary: JavaUILibrary

  override fun setUp() {
    super.setUp()
    initialUiLibrary = EduSettings.getInstance().javaUiLibrary
    EduSettings.getInstance().setJavaUiLibrary(JavaUILibrary.JCEF, changedByUser = false)
  }

  override fun tearDown() {
    try {
      EduSettings.getInstance().setJavaUiLibrary(initialUiLibrary, changedByUser = false)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test the form is pointed out in the file of a quiz`() {
    val course = createQuizCourse()
    checkEditorNotification(
      file(course, "choice", "Main.java"),
      QuizFormNotificationProvider::class.java,
      EduCoreBundle.message("hyperskill.quiz.answer.in.form")
    )
  }

  @Test
  fun `test the form is pointed out in a file of a quiz answered in the form`() {
    val course = createQuizCourse()
    checkEditorNotification(file(course, "dataset", "dataset.txt"), QuizFormNotificationProvider::class.java)
  }

  @Test
  fun `test the form is not pointed out in the files a quiz is answered with`() {
    val course = createQuizCourse()
    checkNoEditorNotification<QuizFormNotificationProvider>(file(course, "sql", QuizTask.SQL_FILE_NAME))
    checkNoEditorNotification<QuizFormNotificationProvider>(file(course, "html", QuizTask.HTML_FILE_NAME))
    checkNoEditorNotification<QuizFormNotificationProvider>(file(course, "html", QuizTask.CSS_FILE_NAME))
  }

  @Test
  fun `test the form is not pointed out in a code task`() {
    val course = createQuizCourse()
    checkNoEditorNotification<QuizFormNotificationProvider>(file(course, "code", "Main.java"))
  }

  @Test
  fun `test the form is not pointed out without JCEF`() {
    EduSettings.getInstance().setJavaUiLibrary(JavaUILibrary.SWING, changedByUser = false)
    val course = createQuizCourse()
    checkNoEditorNotification<QuizFormNotificationProvider>(file(course, "choice", "Main.java"))
  }

  private fun createQuizCourse(): Course = courseWithFiles {
    lesson("lesson") {
      quizTask("choice", HyperskillTaskType.CHOICE.type) {
        taskFile("Main.java")
      }
      quizTask("dataset", HyperskillTaskType.DATASET.type) {
        taskFile("dataset.txt")
      }
      quizTask("sql", HyperskillTaskType.SQL.type) {
        taskFile(QuizTask.SQL_FILE_NAME)
      }
      quizTask("html", HyperskillTaskType.HTML.type) {
        taskFile(QuizTask.HTML_FILE_NAME)
        taskFile(QuizTask.CSS_FILE_NAME)
      }
      eduTask("code") {
        taskFile("Main.java")
      }
    }
  }

  private fun file(course: Course, taskName: String, fileName: String): VirtualFile {
    val task = course.lessons.single().getTask(taskName) ?: error("No task $taskName")
    return task.getTaskFile(fileName)?.getVirtualFile(project) ?: error("No file $fileName in $taskName")
  }
}
