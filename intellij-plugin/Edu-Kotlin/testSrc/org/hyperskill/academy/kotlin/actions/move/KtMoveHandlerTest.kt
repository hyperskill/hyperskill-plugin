package org.hyperskill.academy.kotlin.actions.move

import com.intellij.psi.PsiElement
import org.hyperskill.academy.learning.actions.move.MoveHandlerTestBase
import org.hyperskill.academy.learning.courseFormat.Course
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtMoveHandlerTest : MoveHandlerTestBase(KotlinLanguage.INSTANCE) {

  @Test
  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/Bar.kt") }
    doTest(findTarget) {
      kotlinTaskFile(
        "Foo.kt", """
        fun foo<caret>() {}
      """
      )
      kotlinTaskFile("Bar.kt")
    }
  }

  @Test
  fun `test do not forbid move refactoring for classes`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/Bar.kt") }
    doTest(findTarget) {
      kotlinTaskFile(
        "Foo.kt", """
        class Foo<caret>
      """
      )
      kotlinTaskFile("Bar.kt")
    }
  }
}
