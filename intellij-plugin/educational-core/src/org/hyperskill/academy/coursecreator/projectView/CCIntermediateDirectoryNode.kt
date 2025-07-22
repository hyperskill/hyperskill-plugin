package org.hyperskill.academy.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.projectView.IntermediateDirectoryNode

class CCIntermediateDirectoryNode(
  project: Project,
  course: Course,
  value: PsiDirectory,
  viewSettings: ViewSettings,
) : CCContentHolderNode, IntermediateDirectoryNode(project, value, viewSettings, course)