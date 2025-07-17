package com.jetbrains.edu.scala.courseGeneration

import com.jetbrains.edu.jvm.courseGeneration.JvmCourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
import org.junit.Test

class ScalaGradleCourseBuilderTest : JvmCourseGenerationTestBase() {

  @Test
  fun `test study course structure`() {
    generateCourseStructure("testData/newCourse/scala_course.json")
    val expectedFileTree = fileTree {
      dir("lesson1") {
        dir("my task 1") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("Test.scala")
          }
          file("task.html")
        }
        dir("my task 2") {
          dir("src") {
            file("Task.scala")
          }
          dir("test") {
            file("Test.scala")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedFileTree.assertEquals(rootDir)
  }
}
