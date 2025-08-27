package org.hyperskill.academy.coursecreator.yaml

import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.yaml.YamlFormatSettings.YAML_TEST_PROJECT_READY
import org.hyperskill.academy.learning.yaml.YamlFormatSettings.YAML_TEST_THROW_EXCEPTION
import org.hyperskill.academy.learning.yaml.YamlTestCase
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.HYPERSKILL_TYPE_YAML
import org.junit.Test

class YamlTypeChangedTest : YamlTestCase() {

  override fun setUp() {
    super.setUp()
    project.putUserData(YAML_TEST_PROJECT_READY, false)
  }

  @Test
  fun `test edu to hyperskill course`() {
    project.putUserData(YAML_TEST_THROW_EXCEPTION, false)
    testCourseTypeChanged(HYPERSKILL_TYPE_YAML, HyperskillCourse::class.java)
  }

  private fun <T : Course> testCourseTypeChanged(courseType: String, expectedCourse: Class<T>) {
    val course = getCourse()
    loadItemFromConfig(
      course, """
      |type: $courseType
      |title: Kotlin Course41
      |language: English
      |summary: test
      |programming_language: Plain text
      |content:
      |- lesson1
      |""".trimMargin()
    )

    val loadedCourse = getCourse()
    assertInstanceOf(loadedCourse, expectedCourse)
    assertEquals(course.items.size, loadedCourse.items.size)
  }

}