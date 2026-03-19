package org.hyperskill.academy.learning.handlers

import com.intellij.psi.PsiManager
import org.hyperskill.academy.learning.EduTestCase
import org.junit.Test

class HandlersUtilsTest : EduTestCase() {

  @Test
  fun `test rename is forbidden for task file`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt")
        }
      }
    }
    // Используем прямой доступ через findFile вместо findFileInTask
    val taskFile = findFile("lesson1/task1/Task.kt")
    val psiFile = PsiManager.getInstance(project).findFile(taskFile)!!

    assertTrue("Renaming a task file should be forbidden", isRenameForbidden(project, psiFile))
  }

  @Test
  fun `test rename is not forbidden for non-task file`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt")
        }
      }
    }
    val nonTaskFile = myFixture.addFileToProject("lesson1/task1/non-task-file.kt", "").virtualFile
    val psiFile = PsiManager.getInstance(project).findFile(nonTaskFile)!!

    assertFalse("Renaming a non-task file should be allowed", isRenameForbidden(project, psiFile))
  }

  @Test
  fun `test rename is not forbidden outside of course project`() {
    // No course set in project
    val file = myFixture.addFileToProject("some/file.kt", "").virtualFile
    val psiFile = PsiManager.getInstance(project).findFile(file)!!

    assertFalse("Renaming a file outside a course project should be allowed", isRenameForbidden(project, psiFile))
  }
}
