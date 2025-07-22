// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.hyperskill.academy.learning.courseView

import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
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
