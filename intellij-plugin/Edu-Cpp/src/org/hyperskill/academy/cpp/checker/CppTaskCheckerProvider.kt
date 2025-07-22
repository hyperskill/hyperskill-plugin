package org.hyperskill.academy.cpp.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.CodeExecutor
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

open class CppTaskCheckerProvider : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = CppCodeExecutor()

  // TODO implement envChecker validation
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = CppEduTaskChecker(task, envChecker, project)
}

class CppGTaskCheckerProvider : CppTaskCheckerProvider() {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = CppGEduTaskChecker(task, envChecker, project)
}

class CppCatchTaskCheckerProvider : CppTaskCheckerProvider() {
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = CppCatchEduTaskChecker(task, envChecker, project)
}
