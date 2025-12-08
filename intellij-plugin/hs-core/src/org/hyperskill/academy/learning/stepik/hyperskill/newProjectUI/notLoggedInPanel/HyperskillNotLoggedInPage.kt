package org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel

import com.intellij.ui.JBColor
import com.intellij.ui.NewUI
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.ScrollPaneConstants

class HyperskillNotLoggedInPanel : Wrapper() {
  private val backgroundColor = JBColor.namedColor(
    "SelectCourse.Hyperskill.HyperskillNotLoggedInPanel.backgroundColor", 0xFFFFFF, 0x1E1F22
  )

  private val oldUIBackgroundColor = JBColor(0xFFFFFF, 0x313335)

  init {
    val loginPanel = HyperskillTopLoginPanelWithBanner()
    val howItWorksPanel = HowItWorksPanel()

    // Build the content panel first to preserve background and styling
    val contentPanel = panel {
      row {
        cell(loginPanel).align(AlignX.FILL)
      }
      row {
        cell(howItWorksPanel).align(AlignX.CENTER)
      }
    }.apply {
      isOpaque = true
      background = if (NewUI.isEnabled()) backgroundColor else oldUIBackgroundColor
    }

    // Wrap content into a scroll pane: vertical as needed, horizontal never
    val scrollPane = ScrollPaneFactory.createScrollPane(contentPanel, true).apply {
      horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
      verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
      isOpaque = false
      border = null
      viewport.isOpaque = false
    }

    setContent(scrollPane)
  }
}
