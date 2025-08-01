package org.hyperskill.academy.coursecreator.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import org.hyperskill.academy.learning.canBeAddedToTask
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.getContainingTask
import org.hyperskill.academy.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import org.hyperskill.academy.learning.messages.EduCoreBundle.message
import org.hyperskill.academy.learning.projectView.CourseViewUtils.testPresentation
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.isConfigFile
import org.jetbrains.annotations.TestOnly

/**
 * Add to the file name postfix "course.creator.course.view.excluded" from EduCoreBundle.properties
 * if the file in .courseignore
 */
class CCStudentInvisibleFileNode(
  project: Project,
  value: PsiFile,
  viewSettings: ViewSettings,
  private val name: String = value.name
) : CCFileNode(project, value, viewSettings) {

  private fun needsExcludedMark(file: VirtualFile?, project: Project): Boolean {
    file ?: return false
    val task = file.getContainingTask(project)

    return if (task != null) {
      file.canBeAddedToTask(project)
    }
    else {
      val course = project.course ?: return false
      !containsAdditionalFile(course, file) && !generatedPersonallyForStudent(file)
    }
  }

  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)

    val file = value.virtualFile
    val isExcluded = needsExcludedMark(file, project)
    val presentableName = if (isExcluded) message("course.creator.course.view.excluded", name) else name

    data.clearText()
    data.addText(presentableName, SimpleTextAttributes.GRAY_ATTRIBUTES)
  }

  @Deprecated(
    "Deprecated in Java",
    ReplaceWith("testPresentation(this)", "org.hyperskill.academy.learning.projectView.CourseViewUtils.testPresentation")
  )
  @TestOnly
  override fun getTestPresentation(): String {
    return testPresentation(this)
  }

  private fun containsAdditionalFile(course: Course, file: VirtualFile): Boolean {
    val relativePath = FileUtil.getRelativePath(
      project.courseDir.path,
      file.path,
      VFS_SEPARATOR_CHAR
    )
    return course.additionalFiles.any { it.name == relativePath }
  }

  private fun generatedPersonallyForStudent(file: VirtualFile): Boolean =
  // TODO should be delegated to [configurator] after EDU-7821 is implemented
    // task.md and task.html are also generated personally for a student, but this method is called only for files outside tasks
    isConfigFile(file)
    || file.name == LOCAL_PROPERTIES // for android configurator
    || file.extension?.lowercase() == "sln" // for C# configurator
}
