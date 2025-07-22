package org.hyperskill.academy.csharp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.ideaInterop.fileTypes.msbuild.CsprojFileType
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateTargetFramework
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
import com.jetbrains.rider.projectView.workspace.findProjectsByName
import com.jetbrains.rider.projectView.workspace.getSolutionEntity
import org.hyperskill.academy.learning.capitalize
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.getLesson
import org.hyperskill.academy.learning.getSection

val DEFAULT_DOT_NET = ProjectTemplateTargetFramework.latest.presentation

fun Task.csProjPathByTask(project: Project): String = GeneratorUtils.joinPaths(getDir(project.courseDir)?.path, getCSProjFileName())

fun getDotNetVersion(version: String?): String = version ?: DEFAULT_DOT_NET

fun Task.getCSProjFileName(): String = "${getCSProjFileNameWithoutExtension()}.${CsprojFileType.defaultExtension}"

fun Task.getCSProjFileNameWithoutExtension(): String = pathInCourse.formatForCSProj()

fun String.formatForCSProj(): String = split("/").joinToString(".") { it.capitalize() }

fun Task.getTestName(): String = getCSProjFileNameWithoutExtension().toTestName()

fun String.toTestName(): String = filter { it != '.' } + "Test"
fun Task.toProjectModelEntity(project: Project): ProjectModelEntity? =
  WorkspaceModel.getInstance(project).findProjectsByName(getCSProjFileNameWithoutExtension()).firstOrNull()

fun Project.getSolutionEntity(): ProjectModelEntity? = WorkspaceModel.getInstance(this).getSolutionEntity()

fun includeTopLevelDirsInCourseView(project: Project, course: Course) {
  val filesToIndex = project.courseDir.children.filter { it.isTopLevelDirectory(project, course) }.mapNotNull { it.toIOFile() }
  CSharpBackendService.getInstance(project).includeFilesToCourseView(filesToIndex)
}

fun VirtualFile.isTopLevelDirectory(project: Project, course: Course): Boolean {
  return getSection(project) != null || getLesson(project) != null
         || course.configurator?.shouldFileBeVisibleToStudent(this) == true
}