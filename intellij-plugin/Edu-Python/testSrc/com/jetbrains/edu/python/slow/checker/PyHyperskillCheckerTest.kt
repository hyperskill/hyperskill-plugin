package org.hyperskill.academy.python.slow.checker

import com.intellij.util.ThrowableRunnable
import com.jetbrains.python.PythonLanguage
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.checker.CheckActionListener
import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyHyperskillCheckerTest : PyCheckersTestBase() {

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    // Hyperskill python support is not available in Android Studio
    if (!EduUtilsKt.isAndroidStudio()) {
      super.runTestRunnable(context)
    }
  }

  override fun createCourse(): Course {
    val course = course(courseProducer = ::HyperskillCourse, language = PythonLanguage.INSTANCE) {
      frameworkLesson {
        eduTask("Edu") {
          pythonTaskFile("hello_world.py", """print("Hello, world! My name is type your name")""")
          pythonTaskFile("tests.py", """print("#educational_plugin test OK")""")
        }
      }
    } as HyperskillCourse
    course.stages = listOf(HyperskillStage(1, "", 1))
    course.hyperskillProject = HyperskillProject()
    return course
  }

  @Test
  fun testPythonCourse() {
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    doTest()
  }
}
