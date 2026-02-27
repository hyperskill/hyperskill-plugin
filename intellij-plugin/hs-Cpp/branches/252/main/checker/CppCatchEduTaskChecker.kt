package org.hyperskill.academy.cpp.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.execution.testing.tcatch.CMakeCatchTestRunConfigurationType
import org.hyperskill.academy.cpp.CppCatchCourseBuilder
import org.hyperskill.academy.cpp.CppConfigurator
import org.hyperskill.academy.cpp.CppProjectSettings
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

class CppCatchConfigurator : CppConfigurator() {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppCatchCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider =
    object : CppTaskCheckerProvider() {
      override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
        object : CppEduTaskChecker(task, envChecker, project) {
          override fun getFactory(): ConfigurationFactory =
            CMakeCatchTestRunConfigurationType.getInstance().getFactory()
        }
    }
}
