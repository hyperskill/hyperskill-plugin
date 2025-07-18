package com.jetbrains.edu.cpp.courseGeneration

import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import org.junit.Test

class CppCourseBuilderTest : CourseGenerationTestBase<CppProjectSettings>() {

  override val defaultSettings = CppProjectSettings()

  @Test
  fun `test study edu course structure with top-level lesson`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      lesson("lesson") {
        eduTask("task") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
      }
      additionalFiles {
        eduFile("CMakeLists.txt.in")
        eduFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
    }.assertEquals(rootDir)

  }

  @Test
  fun `test study edu course structure with section`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      section("section") {
        lesson("lesson") {
          eduTask("task") {
            taskFile("src/task.cpp")
            taskFile("test/test.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }
      additionalFiles {
        eduFile("CMakeLists.txt.in")
        eduFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section/lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
    }.assertEquals(rootDir)

  }

  @Test
  fun `test study course structure with top-level section and lesson`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      section("section") {
        lesson("lesson") {
          eduTask("task") {
            taskFile("src/task.cpp")
            taskFile("test/test.cpp")
            taskFile("CMakeLists.txt")
          }
        }
      }
      lesson("top_level_lesson") {
        eduTask("task") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
      }
      additionalFiles {
        eduFile("CMakeLists.txt.in")
        eduFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("section/lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt")
      }
      dir("top_level_lesson/task") {
        dir("src") {
          file("task.cpp")
        }
        dir("test") {
          file("test.cpp")
        }
        file("task.html")
        file("CMakeLists.txt")
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
    }.assertEquals(rootDir)

  }

  @Test
  fun `test study edu course structure with different tasks`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      lesson("lesson") {
        eduTask("edu") {
          taskFile("src/task.cpp")
          taskFile("test/test.cpp")
          taskFile("CMakeLists.txt")
        }
        outputTask("output") {
          taskFile("src/task.cpp")
          taskFile("output.txt")
          taskFile("CMakeLists.txt")
        }
        theoryTask("theory") {
          taskFile("src/task.cpp")
          taskFile("CMakeLists.txt")
        }
      }

      additionalFiles {
        eduFile("CMakeLists.txt.in")
        eduFile("CMakeLists.txt")
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson") {
        dir("edu") {
          dir("src") {
            file("task.cpp")
          }
          dir("test") {
            file("test.cpp")
          }
          file("task.html")
          file("CMakeLists.txt")
        }
        dir("output") {
          dir("src") {
            file("task.cpp")
          }
          file("output.txt")
          file("task.html")
          file("CMakeLists.txt")
        }
        dir("theory") {
          dir("src") {
            file("task.cpp")
          }
          file("task.html")
          file("CMakeLists.txt")
        }
      }
      file("CMakeLists.txt.in")
      file("CMakeLists.txt")
    }.assertEquals(rootDir)
  }

  @Test
  fun `test study edu course creates nothing if CMakeLists already exists`() {
    val course = course(language = OCLanguage.getInstance(), environment = "Catch") {
      additionalFiles {
        eduFile("CMakeLists.txt", "file 1")
        eduFile("cmake/utils.cmake", "file 2")
        // cmake/catch.cmake is skipped to test it will not be created
      }
    }
    createCourseStructure(course)
    assertListOfAdditionalFiles(
      course,
      "CMakeLists.txt" to "file 1",
      "cmake/utils.cmake" to "file 2"
    )
  }
}
