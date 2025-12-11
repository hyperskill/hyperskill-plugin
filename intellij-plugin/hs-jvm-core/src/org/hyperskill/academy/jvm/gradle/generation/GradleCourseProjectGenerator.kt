package org.hyperskill.academy.jvm.gradle.generation

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VfsUtil
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.runInWriteActionAndWait
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator

open class GradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {

  override fun afterProjectGenerated(
    project: Project,
    projectSettings: JdkProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    val jdk = projectSettings.setUpProjectJdk(project, course, ::getJdk)
    setupGradleSettings(project, jdk)
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }

  protected open fun setupGradleSettings(project: Project, sdk: Sdk?) {
    EduGradleUtils.setGradleSettings(project, sdk, project.basePath!!)
  }

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>) {
    super.createAdditionalFiles(holder)
    // Create util module directory with src subdirectory.
    // Gradle 9.x requires module directories to exist during project configuration.
    // The util module is referenced in settings.gradle and build.gradle templates.
    runInWriteActionAndWait {
      VfsUtil.createDirectoryIfMissing(holder.courseDir, "$UTIL_MODULE_NAME/src")
    }
  }

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    val gradleCourseBuilder = courseBuilder as GradleCourseBuilderBase
    if (EduGradleUtils.hasCourseHaveGradleKtsFiles(holder.course)) {
      return emptyList()
    }
    return EduGradleUtils.createProjectGradleFiles(
      holder,
      gradleCourseBuilder.templates(holder.course),
      gradleCourseBuilder.templateVariables(holder.courseDir.name)
    )
  }

  protected open fun getJdk(settings: JdkProjectSettings): Sdk? {
    return settings.jdk
  }

  override suspend fun prepareToOpen(project: Project, module: Module) {
    super.prepareToOpen(project, module)
    @Suppress("UnstableApiUsage")
    writeAction { GeneratorUtils.removeModule(project, module) }
    PropertiesComponent.getInstance(project).setValue(SHOW_UNLINKED_GRADLE_POPUP, false, true)
  }

  companion object {
    private const val SHOW_UNLINKED_GRADLE_POPUP = "show.inlinked.gradle.project.popup"
    private const val UTIL_MODULE_NAME = "util"
  }
}
