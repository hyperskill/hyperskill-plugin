package org.hyperskill.academy.scala.actions.move

import com.intellij.psi.PsiElement
import org.hyperskill.academy.learning.actions.move.MoveHandlerTestBase
import org.hyperskill.academy.learning.courseFormat.Course
import org.jetbrains.plugins.scala.ScalaLanguage
import org.junit.Test

class ScalaMoveHandlerTest : MoveHandlerTestBase(ScalaLanguage.INSTANCE, "sbt") {

  @Test
  fun `test do not forbid move refactoring for functions`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/Bar.scala") }
    doTest(findTarget) {
      scalaTaskFile(
        "Foo.scala", """
        def foo<caret>() {}
      """
      )
      scalaTaskFile("Bar.scala")
    }
  }

  @Test
  fun `test do not forbid move refactoring for classes`() {
    val findTarget: (Course) -> PsiElement = { findPsiFile("lesson1/task1/Bar.scala") }
    doTest(findTarget) {
      scalaTaskFile(
        "Foo.scala", """
        class Foo<caret> {}
      """
      )
      scalaTaskFile("Bar.scala")
    }
  }
}
