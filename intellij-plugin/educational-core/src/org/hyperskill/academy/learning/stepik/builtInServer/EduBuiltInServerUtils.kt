package org.hyperskill.academy.learning.stepik.builtInServer

import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.invokeLater
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import org.hyperskill.academy.learning.yaml.YamlDeepLoader.loadRemoteInfo
import org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeCourse
import org.hyperskill.academy.learning.yaml.YamlMapper
import java.io.File

object EduBuiltInServerUtils {

  fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    val openProjects = ProjectManager.getInstance().openProjects
    for (project in openProjects) {
      if (project.isDefault) continue
      val course = project.course ?: continue
      if (!coursePredicate(course)) continue
      project.invokeLater { project.requestFocus() }
      return project to course
    }
    return null
  }

  private fun openProject(projectPath: String): Project? {
    var project: Project? = null
    ApplicationManager.getApplication().invokeAndWait {
      project = ProjectUtil.openProject(projectPath, null, true)
      project?.requestFocus()
    }
    return project
  }

  private fun Project.requestFocus() = ProjectUtil.focusProjectWindow(this, true)

  fun openRecentProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    val recentPaths = RecentProjectsManagerBase.getInstanceEx().getRecentPaths()

    for (projectPath in recentPaths) {
      val course = try {
        getCourseFromYaml(projectPath) ?: continue
      }
      catch (e: Exception) {
        thisLogger().warn("Failed to load course meta-information from $projectPath", e)
        continue
      }
      if (coursePredicate(course)) {
        val project = openProject(projectPath) ?: continue
        val realProjectCourse = project.course ?: continue
        return project to realProjectCourse
      }
    }
    return null
  }

  private fun getCourseFromYaml(projectPath: String): Course? {
    val projectFile = File(PathUtil.toSystemDependentName(projectPath))
    val projectDir = VfsUtil.findFile(projectFile.toPath(), true) ?: return null
    val remoteInfoConfig = projectDir.findChild(REMOTE_COURSE_CONFIG) ?: return null
    val localCourseConfig = projectDir.findChild(COURSE_CONFIG) ?: return null
    return runReadAction {
      val localCourse = ProgressManager.getInstance().computeInNonCancelableSection<Course, Exception> {
        YamlMapper.basicMapper().deserializeCourse(VfsUtil.loadText(localCourseConfig))
      } ?: return@runReadAction null
      localCourse.loadRemoteInfo(remoteInfoConfig)
      localCourse
    }
  }
}
