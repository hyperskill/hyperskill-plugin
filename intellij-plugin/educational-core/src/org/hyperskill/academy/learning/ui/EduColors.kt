package org.hyperskill.academy.learning.ui

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.editor.EditorBundle
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color

object EduColors {
  val errorTextForeground: Color = JBColor.namedColor("Component.errorForeground", 0xac0013, 0xef5f65)
  val warningTextForeground: Color = JBColor.namedColor("Component.warningForeground", 0xa49152, 0xbbb529)
  val correctLabelForeground: Color = JBColor.namedColor("Submissions.CorrectLabel.foreground", 0x368746, 0x499C54)

  // navigation map colors
  val navigationMapIconNotSelectedBorder: JBColor = JBColor.namedColor("NavigationMap.icon.not.selected.border", 0xC9CCD6, 0x646464)
  val navigationMapIconSelectedBorder: JBColor = JBColor.namedColor("NavigationMap.icon.selected.border", 0x3574F0, 0x3574F0)
  val navigationMapIconSolvedBorder: JBColor = JBColor.namedColor("NavigationMap.icon.solved.border", 0x369650, 0x5FAD65)
  val navigationMapDisabledIconBackground: JBColor = JBColor.namedColor("NavigationMap.disabledIconBackground", 0xEBECF0, 0x4E5157)
  val navigationMapDisabledIconForeground: JBColor = JBColor.namedColor("NavigationMap.disabledIconForeground", 0xA8ADBD, 0x868A91)

  val taskToolWindowLessonLabel: JBColor = JBColor.namedColor("TaskToolWindow.lessonNameForeground", 0x6C707E, 0x6F737A)

  val wrongLabelForeground: Color = UIUtil.getErrorForeground()
  val hyperlinkColor: Color get() = getColorFromThemeIfNeeded("Link.activeForeground", JBColor(0x6894C6, 0x5C84C9))

  @Suppress("SameParameterValue")
  private fun getColorFromThemeIfNeeded(colorProperty: String, customColor: Color): Color {
    if (getCurrentThemeName() == EditorBundle.message("intellij.light.color.scheme.name")) {
      return customColor
    }
    return JBColor.namedColor(colorProperty, customColor)
  }

  fun getCurrentThemeName(): String? = LafManager.getInstance().currentUIThemeLookAndFeel?.name
}
