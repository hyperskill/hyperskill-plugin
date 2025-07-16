package com.jetbrains.edu.yaml.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.patterns.PsiElementPattern
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.coursecreator.StudyItemType.*
import com.jetbrains.edu.coursecreator.failedToFindItemMessage
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.hasSections
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import com.jetbrains.edu.yaml.ItemContainerContentReferenceProvider
import org.jetbrains.yaml.psi.YAMLScalar

class StudyItemNotFoundInspection : UnresolvedFileReferenceInspection() {
  override val pattern: PsiElementPattern.Capture<YAMLScalar> get() = ItemContainerContentReferenceProvider.PSI_PATTERN
  override val supportedConfigs: List<String> = listOf(LESSON_CONFIG, SECTION_CONFIG, COURSE_CONFIG)

  override fun registerProblem(holder: ProblemsHolder, element: YAMLScalar) {
    val childType = when (element.itemType) {
      COURSE_TYPE -> {
        val course = element.project.course ?: return
        if (course.hasSections) SECTION_TYPE else LESSON_TYPE
      }

      SECTION_TYPE -> LESSON_TYPE
      LESSON_TYPE -> TASK_TYPE
      else -> return
    }

    val message = childType.failedToFindItemMessage(element.textValue)
    val fix = null
    holder.registerProblem(element, message, ProblemHighlightType.LIKE_UNKNOWN_SYMBOL, *listOfNotNull(fix).toTypedArray())
  }

  private val YAMLScalar.itemType: StudyItemType
    get() {
      return when (containingFile.name) {
        COURSE_CONFIG -> COURSE_TYPE
        SECTION_CONFIG -> SECTION_TYPE
        LESSON_CONFIG -> LESSON_TYPE
        else -> error("Unexpected containing file `${containingFile.name}`")
      }
    }
}
