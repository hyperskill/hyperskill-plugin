package org.hyperskill.academy.csharp.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

class CSharpTaskCheckerProvider : TaskCheckerProvider {
  override fun getEduTaskChecker(task: EduTask, project: Project): CSharpEduTaskChecker =
    CSharpEduTaskChecker(task, envChecker, project)

  override val envChecker: EnvironmentChecker
    get() = CSharpEnvironmentChecker()
}