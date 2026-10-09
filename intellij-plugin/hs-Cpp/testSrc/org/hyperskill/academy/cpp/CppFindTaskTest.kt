package org.hyperskill.academy.cpp

import org.hyperskill.academy.learning.FindTaskFileTestBase
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.Course
import org.junit.Test

class CppFindTaskTest : FindTaskFileTestBase<CppProjectSettings>() {

  override val defaultSettings = CppProjectSettings()

  override fun tearDown() {
    try {
      revertNovaCodeInsightSettings()
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test get task dir from top-level lesson`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskDir(
      filePath = "./lesson1/task2/src/task.cpp",
      taskDirPath = "./lesson1/task2"
    )
  }

  @Test
  fun `test get task dir from top-level section`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskDir(
      filePath = "./section1/lesson1/task2/src/task.cpp",
      taskDirPath = "./section1/lesson1/task2"
    )
  }

  @Test
  fun `test get task for file from top-level lesson`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskForFile(
      course = courseForTests,
      filePath = "./lesson1/task2/src/task.cpp"
    ) { it.lessons[0].taskList[1] }
  }

  @Test
  fun `test get task for file from top-level section`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskForFile(
      course = courseForTests,
      filePath = "./section1/lesson1/task2/src/task.cpp"
    ) { it.sections[0].lessons[0].taskList[1] }
  }

  @Test
  fun `test get task file from top-level lesson`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskFile(
      course = courseForTests,
      filePath = "./lesson1/task2/src/task.cpp"
    ) { it.lessons[0].taskList[1].taskFiles["src/task.cpp"]!! }
  }

  @Test
  fun `test get task file from top-level section`() {
    val courseForTests = getCourseForTests()
    createCourseStructure(courseForTests)

    doTestGetTaskFile(
      course = courseForTests,
      filePath = "./section1/lesson1/task2/src/task.cpp"
    ) { it.sections[0].lessons[0].taskList[1].taskFiles["src/task.cpp"]!! }
  }

  private fun getCourseForTests(): Course {
    return course(language = cppLanguage, environment = "GoogleTest") {
      section {
        lesson {
          eduTask {
            taskFile("src/task.cpp")
            taskFile("CMakeLists.txt")
          }

          theoryTask {
            taskFile("src/task.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }

      lesson {
        eduTask {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }

        theoryTask {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }
      }

      additionalFiles {
        eduFile("CMakeLists.txt")
      }
    }
  }
}
