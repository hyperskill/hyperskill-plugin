package org.hyperskill.academy.socialMedia.suggestToPostDialog

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import javax.swing.Box
import javax.swing.Icon
import javax.swing.JPanel

/**
 * Center panel of the [SuggestToPostDialog]: a selectable (so it can be copied) achievement message
 * followed by an optional banner image.
 */
class SuggestToPostDialogPanel(message: String, image: Icon?) : JPanel(VerticalFlowLayout(0, 0)) {

  init {
    border = JBUI.Borders.empty()

    val width = image?.iconWidth ?: JBUI.scale(DEFAULT_WIDTH)
    add(MessageTextArea(message, width))

    if (image != null) {
      // Don't use a border for the label because it changes the size of its content
      add(Box.createVerticalStrut(JBUI.scale(10)))
      add(JBLabel(image))
    }
  }

  /**
   * Read-only, word-wrapped text area whose preferred width is pinned to [targetWidth]
   * so the dialog wraps the message to the banner width instead of stretching to a single long line.
   */
  private class MessageTextArea(text: String, private val targetWidth: Int) : JBTextArea(text) {
    init {
      lineWrap = true
      wrapStyleWord = true
      isEditable = false
      isOpaque = false
      border = JBUI.Borders.empty()
    }

    override fun getPreferredSize(): Dimension {
      // Constrain the width first so the height is computed for the wrapped layout
      setSize(targetWidth, Short.MAX_VALUE.toInt())
      return Dimension(targetWidth, super.getPreferredSize().height)
    }
  }

  companion object {
    private const val DEFAULT_WIDTH = 600
  }
}
