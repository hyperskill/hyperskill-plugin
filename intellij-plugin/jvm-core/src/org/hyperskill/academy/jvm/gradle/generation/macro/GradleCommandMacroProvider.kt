package org.hyperskill.academy.jvm.gradle.generation.macro

import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.jvm.gradle.GradleConfiguratorBase
import org.hyperskill.academy.jvm.gradle.checker.getGradleProjectName
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseGeneration.macro.EduMacro
import org.hyperskill.academy.learning.courseGeneration.macro.EduMacroProvider
import org.hyperskill.academy.learning.getContainingTask
import org.hyperskill.academy.learning.isTaskRunConfigurationFile

class GradleCommandMacroProvider : EduMacroProvider {

  override fun provideMacro(holder: CourseInfoHolder<out Course?>, file: VirtualFile): EduMacro? {
    return if (file.isTaskRunConfigurationFile(holder) && holder.course?.configurator is GradleConfiguratorBase) {
      val task = file.getContainingTask(holder) ?: error("Failed to find task for `$file` file")
      val gradleProjectName = getGradleProjectName(task)
      EduMacro(TASK_GRADLE_PROJECT_NAME, gradleProjectName)
    }
    else {
      null
    }
  }

  companion object {
    private const val TASK_GRADLE_PROJECT_NAME = "TASK_GRADLE_PROJECT"
  }
}