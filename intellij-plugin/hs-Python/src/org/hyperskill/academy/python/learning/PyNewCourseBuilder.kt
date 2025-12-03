package org.hyperskill.academy.python.learning

import com.jetbrains.python.PyNames
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor.getVersionStringStatic
import org.hyperskill.academy.coursecreator.actions.TemplateFileInfo
import org.hyperskill.academy.coursecreator.actions.studyItem.NewStudyItemInfo
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.DefaultSettingsUtils.findPath
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.python.learning.PyConfigurator.Companion.MAIN_PY
import org.hyperskill.academy.python.learning.PyConfigurator.Companion.TASK_PY
import org.hyperskill.academy.python.learning.PyNewConfigurator.Companion.TEST_FILE_NAME
import org.hyperskill.academy.python.learning.PyNewConfigurator.Companion.TEST_FOLDER
import org.hyperskill.academy.python.learning.newproject.PyCourseProjectGenerator
import org.hyperskill.academy.python.learning.newproject.PyLanguageSettings
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings
import org.hyperskill.academy.python.learning.newproject.PySdkToCreateVirtualEnv

class PyNewCourseBuilder : EduCourseBuilder<PyProjectSettings> {
  override fun taskTemplateName(course: Course): String = TASK_PY
  override fun mainTemplateName(course: Course): String = MAIN_PY
  override fun testTemplateName(course: Course): String = TEST_FILE_NAME

  override fun getLanguageSettings(): LanguageSettings<PyProjectSettings> = PyLanguageSettings()

  override fun getDefaultSettings(): Result<PyProjectSettings, String> {
    return findPath(INTERPRETER_PROPERTY, "Python interpreter").flatMap { sdkPath ->
      val versionString = getVersionStringStatic(sdkPath)
                          ?: return Err("Can't get python version")
      val sdk = PySdkToCreateVirtualEnv.create(versionString, sdkPath, versionString)
      Ok(PyProjectSettings(sdk))
    }
  }

  override fun getSupportedLanguageVersions(): List<String> = getSupportedVersions()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyProjectSettings> {
    return object : PyCourseProjectGenerator(this@PyNewCourseBuilder, course) {
      override fun createAdditionalFiles(holder: CourseInfoHolder<Course>) {
        // do nothing, independently of what could a base PyCourseProjectGenerator create
      }
    }
  }

  override fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    val templates = ArrayList(super.getDefaultTaskTemplates(course, info, withSources, withTests))
    if (withSources) {
      templates += TemplateFileInfo(PyNames.INIT_DOT_PY, PyNames.INIT_DOT_PY, false)
    }
    if (withTests) {
      templates += TemplateFileInfo(PyNames.INIT_DOT_PY, "$TEST_FOLDER/${PyNames.INIT_DOT_PY}", false)
    }
    return templates
  }

  companion object {
    private const val INTERPRETER_PROPERTY = "project.python.interpreter"
  }
}
