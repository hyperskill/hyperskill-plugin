package org.hyperskill.academy.learning.taskToolWindow.ui.tab

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.taskToolWindow.ui.createTextPane
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.StyleManager
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextPane


class SwingTextPanel(project: Project) : TabTextPanel(project) {
  private var currentLinkHandler: SwingToolWindowLinkHandler? = null

  private val textPane: JTextPane = createTextPane()
  private val textPanel: JBScrollPane = JBScrollPane(textPane)

  override val component: JComponent = textPane

  init {
    textPanel.border = JBUI.Borders.empty()
    add(textPanel, BorderLayout.CENTER)

    updateLinkHandler(null)
  }

  override fun setText(text: String) {
    textPane.text = text
  }

  fun updateLinkHandler(newHyperlinkListener: SwingToolWindowLinkHandler?) {
    textPane.removeHyperlinkListener(currentLinkHandler)
    val linkHandler = newHyperlinkListener ?: SwingToolWindowLinkHandler(project)
    textPane.addHyperlinkListener(linkHandler)
    currentLinkHandler = linkHandler
  }

  fun showLoadingSubmissionsPanel(platformName: String) {
    removeAll()
    val loadingPanel = createLoadingPanel(platformName)
    add(loadingPanel)
    revalidate()
  }

  fun hideLoadingSubmissionsPanel() {
    removeAll()
    add(textPanel)
    revalidate()
  }

  private fun createLoadingPanel(platformName: String): JPanel {
    val loadingPanel = JPanel(FlowLayout(FlowLayout.LEADING))
    val asyncProcessIcon = AsyncProcessIcon("Loading submissions")
    val loadingTextPane: JTextPane = createTextPane().apply {
      @NonNls
      val styledText = "<a ${StyleManager().textStyleHeader}>${EduCoreBundle.message("submissions.loading", platformName)}"
      text = styledText
    }
    loadingPanel.apply {
      background = TaskToolWindowView.getTaskDescriptionBackgroundColor()
      add(asyncProcessIcon)
      add(loadingTextPane)
    }
    return loadingPanel
  }
}
