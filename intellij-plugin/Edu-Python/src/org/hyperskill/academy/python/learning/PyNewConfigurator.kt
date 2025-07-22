package org.hyperskill.academy.python.learning

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.python.learning.PyConfigurator.Companion.TASK_PY
import org.hyperskill.academy.python.learning.checker.PyNewTaskCheckerProvider
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings
import javax.swing.Icon

class PyNewConfigurator : EduConfigurator<PyProjectSettings> {
  override val courseBuilder: PyNewCourseBuilder
    get() = PyNewCourseBuilder()

  override val testFileName: String
    get() = TEST_FILE_NAME

  override fun getMockFileName(course: Course, text: String): String = TASK_PY

  override val testDirs: List<String>
    get() = listOf(TEST_FOLDER)

  override val courseFileAttributesEvaluator: AttributesEvaluator = pythonAttributesEvaluator(super.courseFileAttributesEvaluator)

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PyNewTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Python

  override val defaultPlaceholderText: String
    get() = "# TODO"

  companion object {
    const val TEST_FILE_NAME = "test_task.py"
    const val TEST_FOLDER = "tests"
  }
}
