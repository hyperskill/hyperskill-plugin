package org.hyperskill.academy.learning.actions.move

import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.coursecreator.handlers.move.MoveStudyItemUI
import org.hyperskill.academy.coursecreator.handlers.move.withMockMoveStudyItemUI
import org.hyperskill.academy.coursecreator.ui.CCItemPositionPanel.Companion.AFTER_DELTA
import org.hyperskill.academy.coursecreator.ui.CCItemPositionPanel.Companion.BEFORE_DELTA
import org.hyperskill.academy.learning.EduActionTestCase
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.testAction

abstract class MoveTestBase : EduActionTestCase() {

  protected fun doMoveAction(course: Course, source: PsiElement, targetDir: PsiDirectory, delta: Int? = null) {
    val dataContext = dataContext(source).withTarget(targetDir)
    withMockMoveStudyItemUI(object : MoveStudyItemUI {
      override fun showDialog(project: Project, itemType: StudyItemType, thresholdName: String): Int {
        return when (delta) {
          BEFORE_DELTA, AFTER_DELTA -> delta
          null -> error("Pass `delta` value explicitly")
          else -> error("`delta` value should `$BEFORE_DELTA` or `$AFTER_DELTA`")
        }
      }
    }) {
      withVirtualFileListener(course) {
        testAction(IdeActions.ACTION_MOVE, dataContext)
      }
    }
  }

  protected fun findPsiFile(path: String): PsiFile {
    val file = findFile(path)
    return PsiManager.getInstance(project).findFile(file) ?: error("Failed to find psi file for `$file` file")
  }

  protected fun findPsiDirectory(path: String): PsiDirectory {
    val file = findFile(path)
    return PsiManager.getInstance(project).findDirectory(file) ?: error("Failed to find directory for `$file` file")
  }

  protected fun DataContext.withTarget(element: PsiElement): DataContext {
    return SimpleDataContext.builder()
      .setParent(this)
      .add(LangDataKeys.TARGET_PSI_ELEMENT, element)
      .build()
  }
}
