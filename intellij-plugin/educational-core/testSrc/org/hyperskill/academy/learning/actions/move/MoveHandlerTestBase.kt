package org.hyperskill.academy.learning.actions.move

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.MoveHandlerDelegate
import com.intellij.testFramework.EditorTestUtil
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.TaskBuilder
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.ext.allTasks
import org.hyperskill.academy.learning.courseFormat.ext.getText
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.createCourseFiles

abstract class MoveHandlerTestBase(
  private val language: Language,
  private val environment: String = ""
) : MoveTestBase() {

  override fun createCourse() {}

  private fun List<MoveHandlerDelegate>.checkMoveIsNotForbidden(element: PsiElement, target: PsiElement, dataContext: DataContext) {
    for (handler in this) {
      // EduMoveDelegate should interfere into Move refactoring workflow only if we want to forbid the refactoring
      val mode = project.course?.courseMode
      val refactoringContext = when (mode) {
        CourseMode.STUDENT -> "in learner mode"
        null -> "outside course project"
      }

      val className = handler.javaClass.simpleName
      assertFalse(
        "$className#canMove(DataContext) forbids `Move` refactoring $refactoringContext",
        handler.canMove(dataContext)
      )
      assertFalse(
        "$className#canMove(PsiElement[], PsiElement, PsiReference) forbids `Move` refactoring $refactoringContext",
        handler.canMove(arrayOf(element), target, null)
      )
      assertFalse(
        "$className#tryToMove() prevents `Move` refactoring $refactoringContext",
        handler.tryToMove(element, project, dataContext, null, myFixture.editor)
      )
    }
  }

  protected fun doTest(
    findTarget: (Course) -> PsiElement,
    findElement: (Course) -> PsiElement = ::findElement,
    buildTask: TaskBuilder.() -> Unit
  ) {
    val course = course(language = language, environment = environment) {
      lesson("lesson1") {
        eduTask("task1", buildTask = buildTask)
      }
    }
    course.createCourseFiles(project)

    val element = findElement(course)
    val target = findTarget(course)
    val context = dataContext(element).withTarget(target)

    val eduHandlers = MoveHandlerDelegate.EP_NAME.extensionList.filter { it.javaClass.packageName.startsWith("org.hyperskill.academy") }

    // Check Move is not forbidden without course in the project
    eduHandlers.checkMoveIsNotForbidden(element, target, context)

    // Check Move is not forbidden for course project in learner mode
    StudyTaskManager.getInstance(project).course = course
    eduHandlers.checkMoveIsNotForbidden(element, target, context)
  }

  private fun findElement(course: Course): PsiElement {
    for (task in course.allTasks) {
      for ((_, taskFile) in task.taskFiles) {
        val text = taskFile.getText(project) ?: continue
        if (text.contains(EditorTestUtil.CARET_TAG)) {
          val file = taskFile.getVirtualFile(project) ?: error("")
          myFixture.configureFromExistingVirtualFile(file)
          return myFixture.elementAtCaret
        }
      }
    }
    error("Failed to find caret in course files. Maybe you forgot to add `${EditorTestUtil.CARET_TAG}`?")
  }
}
