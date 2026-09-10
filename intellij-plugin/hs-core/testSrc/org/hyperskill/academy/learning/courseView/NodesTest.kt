// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.hyperskill.academy.learning.courseView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.psi.PsiManager
import com.intellij.ui.SimpleTextAttributes
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.projectView.CourseViewUtils
import org.hyperskill.academy.learning.projectView.TaskNode
import org.junit.Test

class NodesTest : CourseViewTestBase() {

  @Test
  fun testOutsideScrDir() {
    courseWithFiles(language = FakeGradleBasedLanguage) {
      lesson {
        eduTask {
          taskFile("src/file.txt")
          taskFile("test/file.txt")
        }

        eduTask {
          taskFile("src/file.txt")
          taskFile("file1.txt")
          taskFile("test/file.txt")
        }
      }
    }

    assertCourseView(
      """
    |-Project
    | -CourseNode Test Course
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    file.txt
    |   -TaskNode task2
    |    -DirectoryNode src
    |     file.txt
    |    file1.txt
    """.trimMargin("|")
    )
  }

  @Test
  fun `test task directory lookup does not require task getDir`() {
    courseWithFiles(language = FakeGradleBasedLanguage) {
      lesson {
        eduTask {
          taskFile("src/file.txt")
        }
      }
    }

    val task = findTask(0, 0)
    val taskDir = task.getDir(project.courseDir)!!
    val taskPsiDir = PsiManager.getInstance(project).findDirectory(taskDir)!!

    task.name = "missingTask"
    assertNull(task.getDir(project.courseDir))

    val directory = CourseViewUtils.findTaskDirectory(project, taskPsiDir, task)
    assertEquals("src", directory?.name)
  }

  @Test
  fun testSections() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt")
        }
        eduTask {
          taskFile("taskFile3.txt")
        }
        eduTask {
          taskFile("taskFile4.txt")
        }
      }
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
          eduTask {
            taskFile("taskFile2.txt")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
        eduTask {
          taskFile("taskFile2.txt")
        }
      }
    }

    assertCourseView(
      """
    |-Project
    | -CourseNode Test Course
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    taskFile1.txt
    |   -TaskNode task2
    |    taskFile2.txt
    |   -TaskNode task3
    |    taskFile3.txt
    |   -TaskNode task4
    |    taskFile4.txt
    |  -SectionNode section2
    |   -LessonNode lesson1
    |    -TaskNode task1
    |     taskFile1.txt
    |    -TaskNode task2
    |     taskFile1.txt
    |   -LessonNode lesson2
    |    -TaskNode task1
    |     taskFile1.txt
    |    -TaskNode task2
    |     taskFile2.txt
    |  -LessonNode lesson2
    |   -TaskNode task1
    |    taskFile1.txt
    |   -TaskNode task2
    |    taskFile2.txt
    """.trimMargin("|")
    )
  }

  @Test
  fun testTaskFilesOrder() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("C.txt")
          taskFile("B.txt")
          taskFile("A.txt")
        }

        eduTask {
          taskFile("taskFile.txt")
        }
      }
    }

    assertCourseView(
      """
    |-Project
    | -CourseNode Test Course
    |  -LessonNode lesson1
    |   -TaskNode task1
    |    C.txt
    |    B.txt
    |    A.txt
    |   -TaskNode task2
    |    taskFile.txt
    """.trimMargin("|")
    )
  }

  @Test
  fun `test invisible files in student mode`() {
    courseWithInvisibleItems(CourseMode.STUDENT)
    assertCourseView(
      """
      -Project
       -CourseNode Test Course
        -LessonNode lesson1
         -TaskNode task1
          -DirectoryNode folder1
           taskFile3.txt
          taskFile1.txt
         -TaskNode task2
          -DirectoryNode folder
           additionalFile3.txt
          additionalFile1.txt
    """.trimIndent()
    )
  }

  private fun courseWithInvisibleItems(courseMode: CourseMode) {
    courseWithFiles(courseMode = courseMode) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
          taskFile("taskFile2.txt", visible = false)
          dir("folder1") {
            taskFile("taskFile3.txt")
            taskFile("taskFile4.txt", visible = false)
          }
        }
        eduTask {
          taskFile("additionalFile1.txt")
          taskFile("additionalFile2.txt", visible = false)
          dir("folder") {
            taskFile("additionalFile3.txt")
            taskFile("additionalFile4.txt", visible = false)
          }
        }
      }
    }
  }

  private fun createCourseWithTestsInsideTestDir(courseMode: CourseMode = CourseMode.STUDENT) {
    courseWithFiles(courseMode = courseMode) {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
          taskFile("taskFile2.txt")
          dir("tests") {
            taskFile("Tests.txt", visible = false)
          }
        }
        eduTask {
          taskFile("additionalFile1.txt")
          taskFile("additionalFile2.txt")
          dir("folder") {
            taskFile("additionalFile3.txt")
            taskFile("additionalFile4.txt", visible = false)
          }
        }
      }
    }
  }

  @Test
  fun `test student course with tests inside test dir`() {
    createCourseWithTestsInsideTestDir()
    assertCourseView(
      """
      |-Project
      | -CourseNode Test Course
      |  -LessonNode lesson1
      |   -TaskNode task1
      |    taskFile1.txt
      |    taskFile2.txt
      |   -TaskNode task2
      |    -DirectoryNode folder
      |     additionalFile3.txt
      |    additionalFile1.txt
      |    additionalFile2.txt
    """.trimMargin("|")
    )
  }

  @Test
  fun `test hyperskill course`() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask {
          taskFile("file1.txt")
        }
        eduTask {
          taskFile("file2.txt")
        }
      }

      lesson {
        eduTask {
          taskFile("task1.txt")
        }
      }
    }

    findTask(0, 0).status = CheckStatus.Solved

    assertCourseView(
      """
      |-Project
      | -CourseNode Test Course
      |  -FrameworkLessonNode lesson1 1 of 2 stages completed
      |   file1.txt
      |  -LessonNode lesson2
      |   -TaskNode task1
      |    task1.txt
    """.trimMargin()
    )
  }

  @Test
  fun `test task node shows task name when task is detached from its lesson`() {
    courseWithFiles(language = FakeGradleBasedLanguage) {
      lesson {
        eduTask {
          taskFile("src/file.txt")
        }
      }
    }

    val taskDir = findTask(0, 0).getDir(project.courseDir)!!
    val srcDir = PsiManager.getInstance(project).findDirectory(taskDir.findChild("src")!!)!!

    // A task whose parent link is missing, as happens when the course structure fails to be restored
    // from the generated YAML configs (`parent for '<item>' was not found`)
    val detachedTask = EduTask("Abstract class")
    val node = TaskNode(project, srcDir, ViewSettings.DEFAULT, detachedTask)
    node.update()

    assertEquals("TaskNode Abstract class", CourseViewUtils.testPresentation(node))
  }

  /**
   * Mimics `GradleModuleDirectoryDecorator` (IDEA 2026.2+): for a `PsiDirectoryNode` whose directory is a Gradle
   * module content root it wipes the text and renders `<directory> [<module>]`.
   */
  private fun registerModuleDirectoryDecorator() {
    ProjectExtensionPointName<ProjectViewNodeDecorator>("com.intellij.projectViewNodeDecorator").getPoint(project).registerExtension(
      object : ProjectViewNodeDecorator {
        override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
          if (node !is PsiDirectoryNode) return
          val dirName = data.presentableText ?: return
          data.clearText()
          data.addText("$dirName ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
          data.addText("[main]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }
      },
      testRootDisposable
    )
  }

  @Test
  fun `test task node survives a project view decorator rewriting the presentation`() {
    // A task node points at the task source directory, so the decorator turns the task name into `src [main]`
    registerModuleDirectoryDecorator()

    courseWithFiles(language = FakeGradleBasedLanguage) {
      lesson("Abstract class") {
        eduTask("Abstract class") {
          taskFile("src/file.txt")
        }
      }
    }

    assertCourseView(
      """
    |-Project
    | -CourseNode Test Course
    |  -LessonNode Abstract class
    |   -TaskNode Abstract class
    |    file.txt
    """.trimMargin("|")
    )
  }

  @Test
  fun `test directory node survives a project view decorator rewriting the presentation`() {
    // A task with files outside the source directory keeps its own directory, and `src` becomes a directory node of
    // its own -- which the decorator renames just as happily
    registerModuleDirectoryDecorator()

    courseWithFiles(language = FakeGradleBasedLanguage) {
      lesson("Abstract class") {
        eduTask("Abstract class") {
          taskFile("src/file.txt")
          taskFile("file1.txt")
        }
      }
    }

    assertCourseView(
      """
    |-Project
    | -CourseNode Test Course
    |  -LessonNode Abstract class
    |   -TaskNode Abstract class
    |    -DirectoryNode src
    |     file.txt
    |    file1.txt
    """.trimMargin("|")
    )
  }

  @Test
  fun `test hyperskill course with empty framework lesson`() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
      }
    }

    assertCourseView(
      """
      |-Project
      | -CourseNode Test Course
      |  FrameworkLessonNode lesson1
    """.trimMargin()
    )
  }
}
