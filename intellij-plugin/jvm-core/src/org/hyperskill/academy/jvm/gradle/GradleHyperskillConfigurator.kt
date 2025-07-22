package org.hyperskill.academy.jvm.gradle

import com.intellij.openapi.project.Project
import org.hyperskill.academy.jvm.MainFileProvider
import org.hyperskill.academy.jvm.jvmEnvironmentSettings
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.ext.languageById
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.newproject.EduProjectSettings
import org.hyperskill.academy.learning.runReadActionInSmartMode
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator

abstract class GradleHyperskillConfigurator<T : EduProjectSettings>(baseConfigurator: EduConfigurator<T>) :
  HyperskillConfigurator<T>(baseConfigurator) {
  override fun getCodeTaskFile(project: Project, task: Task): TaskFile? {
    val language = task.course.languageById ?: return super.getCodeTaskFile(project, task)
    return runReadActionInSmartMode(project) {
      for (file in task.taskFiles.values) {
        val virtualFile = file.getVirtualFile(project) ?: continue
        if (MainFileProvider.getMainClassName(project, virtualFile, language) != null) return@runReadActionInSmartMode file
      }
      null
    } ?: super.getCodeTaskFile(project, task)
  }

  override fun getEnvironmentSettings(project: Project): Map<String, String> = jvmEnvironmentSettings(project)
}
