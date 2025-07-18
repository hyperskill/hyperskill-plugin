package com.jetbrains.edu.learning.actions.navigate

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.testAction
import org.junit.Test

// Note, `CodeInsightTestFixture#type` can trigger completion (e.g. it inserts paired `"`)
class FrameworkLessonNavigationTest : NavigationTestBase() {

  @Test
  fun `test next`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("fizz.kt")
      myFixture.type("123")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizz.kt", """
            fun fizz() = 123
          """
          )
          file(
            "buzz.kt", """
            fun buzz() = TODO()
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test next in non-marketplace course`() {
    val course = courseWithFiles {
      frameworkLesson {
        eduTask {
          taskFile("task1.kt")
          taskFile("tests/test1.kt")
        }
        eduTask {
          taskFile("task2.kt")
          taskFile("tests/test2.kt")
        }
      }
    }

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("task1.kt")
      myFixture.type("123")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("task2.kt")
          dir("tests") {
            file("test2.kt")
          }
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test next next`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      task.openTaskFileInEditor("fizz.kt")
      myFixture.type("123")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("buzz.kt")
      myFixture.type("456")
      task2.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizzBuzz.kt", """
            fun fizzBuzz() = 123 + 456
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test next prev`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("fizz.kt")
      TaskToolWindowView.getInstance(myFixture.project).currentTask = task
      myFixture.type("123")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)

      TaskToolWindowView.getInstance(myFixture.project).currentTask = myFixture.project.getCurrentTask()
      testAction(PreviousTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizz.kt", """
            fun fizz() = 123
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test opened files`() {
    val course = createFrameworkCourse()

    withVirtualFileListener(course) {
      val task = course.findTask("lesson1", "task1")
      task.openTaskFileInEditor("fizz.kt")
      myFixture.type("123")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    val openFiles = FileEditorManager.getInstance(project).openFiles
    assertEquals(1, openFiles.size)
    assertEquals("buzz.kt", openFiles[0].name)
  }

  @Test
  fun `test navigation to unsolved task`() {
    val course = createFrameworkCourse()

    val task1 = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      // go to the third task without solving prev tasks
      task1.openTaskFileInEditor("fizz.kt")
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("fizz.kt")
      testAction(NextTaskAction.ACTION_ID)
    }

    fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizzBuzz.kt", """
            fun fizzBuzz() = TODO() + TODO()
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }.assertEquals(rootDir, myFixture)

    // Emulate user actions with unsolved dependencies notification
    // and navigate to the first unsolved task
    NavigationUtils.navigateToTask(project, task1, course.lessons[0].getTask("task3"))

    fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizz.kt", """
            fun fizz() = TODO()
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test save student changes`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      task.openTaskFileInEditor("fizz.kt")
      myFixture.type("123")
      myFixture.editor.caretModel.moveToOffset(0)
      myFixture.type("fun foo() {}\n")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)

      TaskToolWindowView.getInstance(myFixture.project).currentTask = myFixture.project.getCurrentTask()
      testAction(PreviousTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizz.kt", """
            fun foo() {}
            fun fizz() = 123
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test save student changes 2`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      task.openTaskFileInEditor("fizz.kt")
      myFixture.type("123")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("buzz.kt")
      myFixture.type("456")
      myFixture.editor.caretModel.moveToOffset(0)
      myFixture.type("fun bar() {}\n")
      testAction(PreviousTaskAction.ACTION_ID)

      TaskToolWindowView.getInstance(myFixture.project).currentTask = myFixture.project.getCurrentTask()
      testAction(NextTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizz.kt", """
            fun fizz() = 123
          """
          )
          file(
            "buzz.kt", """
            fun bar() {}
            fun buzz() = 456
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test do not propagate user created files`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task/foo.kt", "fun foo() {}")
      task.openTaskFileInEditor("fizz.kt")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)

      val task2 = course.findTask("lesson1", "task2")
      task2.openTaskFileInEditor("buzz.kt")
      testAction(PreviousTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizz.kt", """
            fun fizz() = TODO()
          """
          )
          file(
            "foo.kt", """
            fun foo() {}
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test save user created files`() {
    val course = createFrameworkCourse()
    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, rootDir, "lesson1/task/foo.kt", "fun foo() {}")
      task.openTaskFileInEditor("fizz.kt")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file(
            "fizz.kt", """
            fun fizz() = TODO()
          """
          )
          file(
            "buzz.kt", """
            fun buzz() = TODO()
          """
          )
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
        dir("task3") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test empty directories removed`() {
    val course = courseWithFiles {
      frameworkLesson {
        eduTask {
          taskFile("foo/bar/file1.txt")
          taskFile("foo/bar/baz/bar/file2.txt")
        }
        eduTask {
          taskFile("foo/bar/file1.txt")
        }
      }
    }

    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      task.openTaskFileInEditor("foo/bar/file1.txt")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          dir("foo") {
            dir("bar") {
              file("file1.txt")
            }
          }
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test windows line separators`() {
    val course = courseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1") {
          taskFile("file.txt", "a")
        }
        eduTask("task2") {
          taskFile("file.txt") {
            withText("a\r\nb")
          }
        }
      }
    }

    val task = course.findTask("lesson1", "task1")
    withVirtualFileListener(course) {
      task.openTaskFileInEditor("file.txt")
      task.status = CheckStatus.Solved
      testAction(NextTaskAction.ACTION_ID)
    }

    val fileTree = fileTree {
      dir("lesson1") {
        dir("task") {
          file("file.txt", "a\nb")
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
          file("task.md")
        }
      }
    }
    fileTree.assertEquals(rootDir, myFixture)
  }

  @Test
  fun `test editable flag for files remain unchanged when navigating the course`() {
    val course = createCourseWithNonEditableFiles()
    val task1 = course.findTask("lesson", "task1")
    val task2 = course.findTask("lesson", "task2")
    val initialEditableFlags = course.lessons.first().taskList.associate { task ->
      task.name to task.taskFiles.entries.associate { it.key to it.value.isEditable }
    }

    fun checkTask(task: Task) {
      for ((name, file) in task.taskFiles) {
        task.openTaskFileInEditor(name)
        assertTrue(file.isEditable == initialEditableFlags[task.name]?.get(name))
      }
    }

    withVirtualFileListener(course) {
      checkTask(task1)
      testAction(NextTaskAction.ACTION_ID)
      checkTask(task2)
      testAction(PreviousTaskAction.ACTION_ID)
      checkTask(task1)
    }
  }

  @Test
  fun `test non-editable flags when navigate tasks`() {
    val course = createCourseWithNonEditableFiles()

    val task1 = course.findTask("lesson", "task1")
    val task2 = course.findTask("lesson", "task2")

    fun checkTask(task: Task) {
      for ((name, file) in task.taskFiles) {
        task.openTaskFileInEditor(name)
        val virtualFile = file.getVirtualFile(project)
        assertNotNull(virtualFile)
        assertTrue(file.isEditable == virtualFile!!.isWritable)
      }
    }

    withVirtualFileListener(course) {
      checkTask(task1)
      testAction(NextTaskAction.ACTION_ID)
      checkTask(task2)
      testAction(PreviousTaskAction.ACTION_ID)
      checkTask(task1)
    }
  }

  private fun createFrameworkCourse(courseMode: CourseMode = CourseMode.STUDENT): Course = courseWithFiles(courseMode = courseMode) {
    frameworkLesson {
      eduTask {
        taskFile(
          "fizz.kt", """
          fun fizz() = <p>TODO()</p>
        """
        )
      }
      eduTask {
        taskFile(
          "fizz.kt", """
          fun fizz() = <p>TODO()</p>
        """
        )
        taskFile(
          "buzz.kt", """
          fun buzz() = <p>TODO()</p>
        """
        )
      }
      eduTask {
        taskFile(
          "fizzBuzz.kt", """
          fun fizzBuzz() = <p>TODO()</p> + <p>TODO()</p>
        """
        )
      }
    }
  }

  private fun createCourseWithNonEditableFiles() = courseWithFiles(courseMode = CourseMode.STUDENT) {
    frameworkLesson("lesson") {
      eduTask("task1") {
        taskFile(
          name = "mem.kt",
          text = "class Mem {}",
          editable = false,
        )
        taskFile(
          name = "Main.kt",
          text = "fun main() = println(\"Hello world!\")",
          editable = true,
        )
        taskFile(
          name = "file1.kt",
          text = "123",
          editable = true,
        )
        taskFile(
          name = "file2.kt",
          text = "321",
          editable = false,
        )
      }
      eduTask("task2") {
        taskFile(
          name = "mem.kt",
          text = "class Mem { TODO() }",
          editable = true,
        )
        taskFile(
          name = "Main.kt",
          text = "fun main() = println(\"Wow this file had changed\")",
          editable = false,
        )
        taskFile(
          name = "file1.kt",
          text = "321",
          editable = true,
        )
        taskFile(
          name = "file2.kt",
          text = "123",
          editable = false,
        )
      }
    }
  }
}
