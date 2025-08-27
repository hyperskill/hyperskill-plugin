package org.hyperskill.academy.csharp

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import org.hyperskill.academy.csharp.messages.EduCSharpBundle
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import java.awt.BorderLayout
import javax.swing.JComponent

class CSharpLanguageSettings : LanguageSettings<CSharpProjectSettings>() {
  private var targetFrameworkVersion: String = DEFAULT_DOT_NET
  override fun getSettings(): CSharpProjectSettings = CSharpProjectSettings(targetFrameworkVersion)
  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {

    if (course is HyperskillCourse) return emptyList()
    val versions =
      course.configurator?.courseBuilder?.getSupportedLanguageVersions()?.toTypedArray() ?: error("No builder associated with course found")
    val langStandardComboBox = ComboBox(versions)
    val courseTargetFrameworkVersion = course.languageVersion
    if (courseTargetFrameworkVersion != null && versions.contains(courseTargetFrameworkVersion)) {
      targetFrameworkVersion = courseTargetFrameworkVersion
    }
    langStandardComboBox.selectedItem = targetFrameworkVersion

    langStandardComboBox.addItemListener {
      targetFrameworkVersion = it.item.toString()
      notifyListeners()
    }
    return listOf(
      LabeledComponent.create(
        langStandardComboBox,
        EduCSharpBundle.getMessage("target.framework"),
        BorderLayout.WEST
      )
    )
  }
}