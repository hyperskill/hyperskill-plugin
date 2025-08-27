package org.hyperskill.academy.kotlin.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.jvm.gradle.checker.GradleTaskCheckerProvider
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

class KtTaskCheckerProvider : GradleTaskCheckerProvider() {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return KtNewGradleTaskChecker(task, envChecker, project)
  }
}
