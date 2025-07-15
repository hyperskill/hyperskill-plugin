package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Key
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import java.nio.file.Path

data class PreviewInfo(
  /**
   * The path to an archive, from which the preview was loaded
   */
  val previewLoadedFrom: Path,

  /**
   * The base path of the project, from which the preview was created
   */
  val sourceProjectBasePath: String?
) {
  fun findSourceProject(): Project? = ProjectManager.getInstance().openProjects.find {
    it.basePath == sourceProjectBasePath
  }
}

private data class UpdatePreviewActionInfo(
  val previewProject: Project,
  val previewCourse: EduCourse,
  val ccProject: Project,
  val ccCourse: EduCourse,
  val previewInfo: PreviewInfo
) {
  companion object {
    fun fromEvent(e: AnActionEvent): UpdatePreviewActionInfo? {
      val project = e.project ?: return null
      val course = project.course as? EduCourse ?: return null

      if (!course.isStudy) return null
      if (!course.isPreview) return null

      val previewInfo = project.previewInfo ?: return null
      val ccProject = previewInfo.findSourceProject() ?: return null
      val ccCourse = ccProject.course as? EduCourse ?: return null

      return UpdatePreviewActionInfo(project, course, ccProject, ccCourse, previewInfo)
    }
  }
}

private val previewInfoKey = Key<PreviewInfo>("Edu.course.preview.info")

var Project.previewInfo: PreviewInfo?
  get() = getUserData(previewInfoKey)
  set(value) = putUserData(previewInfoKey, value)