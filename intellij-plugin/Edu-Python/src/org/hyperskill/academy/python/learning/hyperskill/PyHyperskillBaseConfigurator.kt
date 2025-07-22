package org.hyperskill.academy.python.learning.stepik.hyperskill

import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.python.learning.PyConfigurator
import org.hyperskill.academy.python.learning.PyCourseBuilder
import org.hyperskill.academy.python.learning.newproject.PyCourseProjectGenerator
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings

/**
 * This class is needed as a hack to override behavior of base configurator during Hyperskill course creation
 *
 */
class PyHyperskillBaseConfigurator : PyConfigurator() {
  override fun getMockFileName(course: Course, text: String): String = MAIN_PY

  override val courseBuilder: EduCourseBuilder<PyProjectSettings>
    get() = PyHyperskillCourseBuilder()

  private class PyHyperskillCourseBuilder : PyCourseBuilder() {
    override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyProjectSettings> {
      return GeneratorWithoutAdditionalFiles(this, course)
    }
  }

  private class GeneratorWithoutAdditionalFiles(builder: PyCourseBuilder, course: Course) : PyCourseProjectGenerator(builder, course) {
    override fun createAdditionalFiles(holder: CourseInfoHolder<Course>) {
      // do nothing, independently of what could a base PyCourseProjectGenerator create
    }
  }
}