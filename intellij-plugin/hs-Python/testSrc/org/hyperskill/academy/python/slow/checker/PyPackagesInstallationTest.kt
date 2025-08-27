package org.hyperskill.academy.python.slow.checker

import com.jetbrains.python.PythonLanguage
import org.hyperskill.academy.learning.checker.CheckActionListener
import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.Course
import org.junit.Test

@Suppress("PyInterpreter", "PyUnresolvedReferences")
class PyPackagesInstallationTest : PyCheckersTestBase() {
  override fun createCourse(): Course {
    return course(language = PythonLanguage.INSTANCE, environment = "unittest") {
      lesson {
        eduTask("Edu") {
          pythonTaskFile(
            "task.py", """
            def sum(a, b):
                return a + b
            """
          )
          dir("tests") {
            taskFile("__init__.py")
            taskFile(
              "tests.py", """
              import unittest
              import requests
              import hstest
              from task import sum
              class TestCase(unittest.TestCase):
                  def test_add(self):
                      self.assertEqual(sum(1, 2), 3, msg="error")
              """
            )
          }
        }
      }
      additionalFile("requirements.txt", "requests\nhttps://github.com/hyperskill/hs-test-python/archive/release.tar.gz")
    }

  }

  @Test
  fun `test required packages installed`() {
    CheckActionListener.expectedMessage { CheckUtils.CONGRATULATIONS }
    doTest()
  }
}