package org.hyperskill.academy.go

import com.intellij.openapi.project.Project
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.go.checker.GoCodeExecutor
import org.hyperskill.academy.go.checker.GoEduTaskChecker
import org.hyperskill.academy.go.checker.GoEnvironmentChecker
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.EduNames.TEST
import org.hyperskill.academy.learning.checker.CodeExecutor
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import javax.swing.Icon

class GoConfigurator : EduConfigurator<GoProjectSettings> {
  override val courseBuilder: EduCourseBuilder<GoProjectSettings>
    get() = GoCourseBuilder()

  override val testFileName: String
    get() = TEST_GO

  override fun getMockFileName(course: Course, text: String): String = TASK_GO

  override val testDirs: List<String>
    get() = listOf(TEST)

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Go

  override val taskCheckerProvider: TaskCheckerProvider
    get() = object : TaskCheckerProvider {
      override val codeExecutor: CodeExecutor
        get() = GoCodeExecutor()
      override val envChecker: EnvironmentChecker
        get() = GoEnvironmentChecker()

      override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = GoEduTaskChecker(project, envChecker, task)
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val TEST_GO = "task_test.go"
    const val TASK_GO = "task.go"
    const val MAIN_GO = "main.go"
    const val GO_MOD = "go.mod"
  }
}
