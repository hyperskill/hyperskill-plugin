@file:Suppress("PyInterpreter")

package org.hyperskill.academy.python.actions.move

import com.intellij.psi.PsiElement
import com.jetbrains.python.PythonLanguage
import org.hyperskill.academy.learning.actions.move.MoveHandlerTestBase
import org.hyperskill.academy.learning.courseFormat.Course
import org.junit.Test

class PyMoveHandlerTest : MoveHandlerTestBase(PythonLanguage.INSTANCE) {

  @Test
  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.py") }
    doTest(findTarget) {
      pythonTaskFile(
        "foo.py", """
        def foo<caret>():
            pass
      """
      )
      pythonTaskFile("bar.py")
    }
  }

  @Test
  fun `test do not forbid move refactoring for classes`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.py") }
    doTest(findTarget) {
      pythonTaskFile(
        "foo.py", """
        class Foo<caret>:
          pass
      """
      )
      pythonTaskFile("bar.py")
    }
  }
}
