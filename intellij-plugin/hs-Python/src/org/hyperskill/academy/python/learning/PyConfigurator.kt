package org.hyperskill.academy.python.learning

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.python.learning.checker.PyTaskCheckerProvider
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings
import javax.swing.Icon

open class PyConfigurator : EduConfigurator<PyProjectSettings> {
  override val courseBuilder: EduCourseBuilder<PyProjectSettings>
    get() = PyCourseBuilder()

  override fun getMockFileName(course: Course, text: String): String = TASK_PY

  override val testFileName: String
    get() = TESTS_PY

  override val courseFileAttributesEvaluator: AttributesEvaluator = pythonAttributesEvaluator(super.courseFileAttributesEvaluator)

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PyTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Python

  override val defaultPlaceholderText: String
    get() = "# TODO"

  companion object {
    const val TESTS_PY = "tests.py"
    const val TASK_PY = "task.py"
    const val MAIN_PY = "main.py"
  }
}
