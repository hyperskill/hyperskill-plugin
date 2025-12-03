package org.hyperskill.academy.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject

/**
 * Condition to check whether a project is an educational project.
 * Used for tool window registration in plugin.xml via conditionClass attribute.
 */
class EduProjectCondition : Condition<Project> {
  override fun value(project: Project): Boolean = project.isEduProject()
}
