@file:JvmName("SwingTaskUtil")

package org.hyperskill.academy.learning.taskToolWindow.ui

import com.intellij.ide.ui.UISettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.project.Project
import com.intellij.util.ui.HTMLEditorKitBuilder
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel.getIconPath
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager.INTELLIJ_ICON_QUICKFIX_OFF_BULB
import org.hyperskill.academy.learning.xmlEscaped
import org.jetbrains.annotations.VisibleForTesting
import org.jsoup.nodes.Element
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit
import kotlin.math.roundToInt

fun createTextPane(editorKit: HTMLEditorKit = HTMLEditorKitBuilder().withWordWrapViewFactory().build()): JTextPane {
  prepareCss(editorKit)

  val textPane = object : JTextPane() {
    override fun getSelectedText(): String {
      // see EDU-3185
      return super.getSelectedText().replace(Typography.nbsp, ' ')
    }
  }

  textPane.contentType = editorKit.contentType
  textPane.editorKit = editorKit
  textPane.isEditable = false
  textPane.background = TaskToolWindowView.getTaskDescriptionBackgroundColor()

  return textPane
}

private fun prepareCss(editorKit: HTMLEditorKit) {
  // ul padding of JBHtmlEditorKit is too small, so copy-pasted the style from
  // com.intellij.codeInsight.documentation.DocumentationComponent.prepareCSS
  editorKit.styleSheet.addRule("ul { padding: 3px 16px 0 0; }")
  editorKit.styleSheet.addRule("li { padding: 3px 0 4px 5px; }")
  editorKit.styleSheet.addRule(".hint { padding: 17px 0 16px 0; }")
}

const val HINT_PROTOCOL = "hint://"
const val CHEVRON_RIGHT = "&#8250"
const val CHEVRON_DOWN = "&#9013"
const val CHEVRON_HTML_CLASS_NAME = "chevron"
const val CHEVRON_RIGHT_HTML_BLOCK = "<span class='$CHEVRON_HTML_CLASS_NAME'>$CHEVRON_RIGHT</span>"
const val CHEVRON_DOWN_HTML_BLOCK = "<span class='$CHEVRON_HTML_CLASS_NAME'>$CHEVRON_DOWN</span>"

private val LOG = Logger.getInstance(SwingToolWindow::class.java)  //TODO we probably need another logger here
private const val DEFAULT_ICON_SIZE = 16

@VisibleForTesting
fun getHintIconSize(): Int {
  val currentFontSize = UISettings.getInstance().fontSize
  val defaultFontSize = FontPreferences.DEFAULT_FONT_SIZE
  return (DEFAULT_ICON_SIZE * currentFontSize / defaultFontSize.toFloat()).roundToInt()
}

fun wrapHintSwing(project: Project, hintElement: Element, displayedHintNumber: String, hintTitle: String): String {
  val bulbWithTheme = getIconPath(INTELLIJ_ICON_QUICKFIX_OFF_BULB.trimStart('/'))
  val bulbIcon = if (!isUnitTestMode) SwingToolWindow::class.java.classLoader.getResource(bulbWithTheme)?.toExternalForm() else ""
  if (bulbIcon == null) LOG.warn("Cannot find bulb icon")

  // all tagged elements should have different href otherwise they are all underlined on hover. That's why
  // we have to add hint number to href
  fun createHintBlockTemplate(hintHtml: String, displayedHintNumber: String, escapedHintTitle: String): String {
    val iconSize = getHintIconSize()
    return """
      <img src='$bulbIcon' width='$iconSize' height='$iconSize' >
      <span><a href='$HINT_PROTOCOL$displayedHintNumber' value='${hintHtml.xmlEscaped}'>$escapedHintTitle $displayedHintNumber</a>
      $CHEVRON_RIGHT_HTML_BLOCK</span>
    """.trimIndent()
  }

  // all tagged elements should have different href otherwise they are all underlined on hover. That's why
  // we have to add hint number to href
  fun createExpandedHintBlockTemplate(hintHtml: String, displayedHintNumber: String, escapedHintTitle: String): String {
    val iconSize = getHintIconSize()
    return """ 
        <img src='$bulbIcon' width='$iconSize' height='$iconSize' >
        <span><a href='$HINT_PROTOCOL$displayedHintNumber' value='${hintHtml.xmlEscaped}'>$escapedHintTitle $displayedHintNumber</a>
        $CHEVRON_DOWN_HTML_BLOCK</span>
        <div class='hint_text'>$hintHtml</div>
     """.trimIndent()
  }

  if (displayedHintNumber.isEmpty() || displayedHintNumber == "1") {
    hintElement.wrap("<div class='top'></div>")
  }
  val course = StudyTaskManager.getInstance(project).course
  val escapedHintTitle = hintTitle.xmlEscaped
  val hintHtml = hintElement.html()
  return if (course != null && !course.isStudy) {
    createExpandedHintBlockTemplate(hintHtml, displayedHintNumber, escapedHintTitle)
  }
  else {
    createHintBlockTemplate(hintHtml, displayedHintNumber, escapedHintTitle)
  }
}

