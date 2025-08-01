package org.hyperskill.academy.go.actions.move

import com.goide.GoLanguage
import com.intellij.psi.PsiElement
import org.hyperskill.academy.learning.actions.move.MoveHandlerTestBase
import org.hyperskill.academy.learning.courseFormat.Course
import org.junit.Test

class GoMoveHandlerTest : MoveHandlerTestBase(GoLanguage.INSTANCE) {

  @Test
  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.go") }
    doTest(findTarget) {
      goTaskFile(
        "foo.go", """
        package task
        func foo<caret>() {}  
      """
      )
      goTaskFile("bar.go")
    }
  }

  @Test
  fun `test do not forbid move refactoring for structs`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/bar.go") }
    doTest(findTarget) {
      goTaskFile(
        "foo.go", """
        package task
        type Foo<caret> struct {}
      """
      )
      goTaskFile("bar.go")
    }
  }
}
