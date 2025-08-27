package org.hyperskill.academy.learning.taskToolWindow.ui.tab

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JCEFHtmlPanel
import org.hyperskill.academy.learning.taskToolWindow.links.JCefToolWindowLinkHandler
import org.hyperskill.academy.learning.taskToolWindow.ui.JCEFTaskInfoLifeSpanHandler
import org.hyperskill.academy.learning.taskToolWindow.ui.JCEFToolWindowRequestHandler
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import java.awt.BorderLayout
import javax.swing.JComponent


class JCEFTextPanel(project: Project) : TabTextPanel(project) {
  private val jcefBrowser = JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null)

  override val component: JComponent
    get() = jcefBrowser.component

  init {
    val toolWindowLinkHandler = JCefToolWindowLinkHandler(project)
    val requestHandler = JCEFToolWindowRequestHandler(toolWindowLinkHandler)
    jcefBrowser.jbCefClient.addRequestHandler(requestHandler, jcefBrowser.cefBrowser)
    val lifeSpanHandler = JCEFTaskInfoLifeSpanHandler(toolWindowLinkHandler)
    jcefBrowser.jbCefClient.addLifeSpanHandler(lifeSpanHandler, jcefBrowser.cefBrowser)
    add(jcefBrowser.component, BorderLayout.CENTER)

    Disposer.register(this, jcefBrowser)
    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(
        LafManagerListener.TOPIC,
        LafManagerListener {
          TaskToolWindowView.updateAllTabs(project)
        })
  }

  override fun setText(text: String) {
    jcefBrowser.loadHTML(text)
  }
}