package org.hyperskill.academy.learning.handlers

import com.intellij.psi.PsiManager
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.junit.Test

class HandlersUtilsTest : EduTestCase() {

  @Test
  fun `test rename is forbidden for task file`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt")
        }
      }
    }
    val task = course.lessons[0].taskList[0]
    val taskFile = task.taskFiles["Task.kt"] ?: error("Task file not found in task")
    val virtualFile = taskFile.getVirtualFile(project) ?: error("Virtual file not found for task file")
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!
    assertTrue("Rename should be forbidden for task file", isRenameForbidden(project, psiFile))
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
    assertFalse("Rename should NOT be forbidden for non-task file", isRenameForbidden(project, psiFile))
  }

  @Test
  fun `test rename is forbidden for task directory`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt")
        }
      }
    }
    val taskDir = findFile("lesson1/task1")
    val psiDir = PsiManager.getInstance(project).findDirectory(taskDir)!!
    assertTrue("Rename should be forbidden for task directory", isRenameForbidden(project, psiDir))
  }

  @Test
  fun `test move is forbidden for task file between tasks`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task1.kt")
        }
        eduTask {
          taskFile("Task2.kt")
        }
      }
    }
    val task1 = course.lessons[0].taskList[0]
    val taskFile1 = task1.taskFiles["Task1.kt"] ?: error("Task file not found in task 1")
    val virtualFile1 = taskFile1.getVirtualFile(project) ?: error("Virtual file not found for task file 1")
    val psiFile1 = PsiManager.getInstance(project).findFile(virtualFile1)!!

    val task2Dir = findFile("lesson1/task2")
    val psiTargetDir = PsiManager.getInstance(project).findDirectory(task2Dir)!!

    assertTrue("Move should be forbidden between different tasks", isMoveForbidden(project, psiFile1, psiTargetDir))
  }

  @Test
  fun `test move is not forbidden for task file within same task`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task1.kt")
          dir("src") {
            taskFile("Task2.kt")
          }
        }
      }
    }
    val task = course.lessons[0].taskList[0]
    val taskFile1 = task.taskFiles["Task1.kt"] ?: error("Task file not found in task")
    val virtualFile1 = taskFile1.getVirtualFile(project) ?: error("Virtual file not found for task file")
    val psiFile1 = PsiManager.getInstance(project).findFile(virtualFile1)!!

    val srcDir = findFile("lesson1/task1/src")
    val psiTargetDir = PsiManager.getInstance(project).findDirectory(srcDir)!!

    assertFalse("Move should NOT be forbidden within the same task", isMoveForbidden(project, psiFile1, psiTargetDir))
  }

  @Test
  fun `test move is not forbidden outside of course project`() {
    // No course set in project
    val file = myFixture.addFileToProject("some/file.kt", "").virtualFile
    val psiFile = PsiManager.getInstance(project).findFile(file)!!
    val targetDir = myFixture.tempDirFixture.findOrCreateDir("target")
    val psiTargetDir = PsiManager.getInstance(project).findDirectory(targetDir)!!

    assertFalse("Move should NOT be forbidden if no course is associated with project", isMoveForbidden(project, psiFile, psiTargetDir))
  }

  @Test
  fun `test rename is not forbidden outside of course project`() {
    // No course set in project
    val file = myFixture.addFileToProject("some/file.kt", "").virtualFile
    val psiFile = PsiManager.getInstance(project).findFile(file)!!

    assertFalse("Rename should NOT be forbidden if no course is associated with project", isRenameForbidden(project, psiFile))
  }

  @Test
  fun `test rename is forbidden for element inside task file`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("Task.kt", "class Task {}")
        }
      }
    }
    val task = course.lessons[0].taskList[0]
    val taskFile = task.taskFiles["Task.kt"] ?: error("Task file not found in task")
    val virtualFile = taskFile.getVirtualFile(project) ?: error("Virtual file not found for task file")
    val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!

    // We simulate an element inside the file (like a class)
    // For this test we can just take the first child of the file if it's not the file itself
    val elementInside = psiFile.firstChild!!

    assertTrue("Rename should be forbidden for element inside task file", isRenameForbidden(project, elementInside))
  }
}
