package org.hyperskill.academy.python.slow.checker

import com.jetbrains.python.PythonLanguage
import org.hyperskill.academy.learning.checker.CheckActionListener
import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.OutputTask
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyCheckersTest : PyCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = PythonLanguage.INSTANCE) {
      lesson {
        eduTask("Edu") {
          pythonTaskFile("hello_world.py", """print("Hello, world! My name is type your name")""")
          pythonTaskFile("tests.py", """print("#educational_plugin test OK")""")
        }
        outputTask("Output") {
          pythonTaskFile("hello_world.py", """print("Hello, World!")""")
          taskFile("output.txt") {
            withText("Hello, World!\n")
          }
        }
        outputTask("Output with input.txt") {
          pythonTaskFile(
            "hello_world.py", """
            a = input()
            print(a + " World!")
          """
          )
          taskFile("output.txt") {
            withText("Hello, World!")
          }
          taskFile("input.txt") {
            withText("Hello,")
          }
        }
        eduTask("NonZeroExitCodeInTests") {
          pythonTaskFile("hello_world.py", """print("Hello, world!")""")
          pythonTaskFile(
            "tests.py", """
            print("#educational_plugin test OK")
            exit(1)
          """
          )
        }
      }
    }
  }

  @Test
  fun `test python course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        else -> null
      }
    }
    doTest()
  }
}
