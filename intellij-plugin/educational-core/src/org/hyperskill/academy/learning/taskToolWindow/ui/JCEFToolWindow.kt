package org.hyperskill.academy.learning.taskToolWindow.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefClient
import com.intellij.ui.jcef.JCEFHtmlPanel
import org.hyperskill.academy.learning.JavaUILibrary
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.taskToolWindow.links.JCefToolWindowLinkHandler
import org.jetbrains.annotations.TestOnly
import javax.swing.JComponent

class JCEFToolWindow(project: Project) : TaskToolWindow(project) {
  private val taskInfoJBCefBrowser = JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null)

  private val taskSpecificJBCefBrowser = JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null)

  init {
    val jcefLinkInToolWindowHandler = JCefToolWindowLinkHandler(project)
    val taskInfoRequestHandler = JCEFToolWindowRequestHandler(jcefLinkInToolWindowHandler)
    val taskInfoLifeSpanHandler = JCEFTaskInfoLifeSpanHandler(jcefLinkInToolWindowHandler)
    taskInfoJBCefBrowser.jbCefClient.apply {
      addRequestHandler(taskInfoRequestHandler, taskInfoJBCefBrowser.cefBrowser)
      addLifeSpanHandler(taskInfoLifeSpanHandler, taskInfoJBCefBrowser.cefBrowser)
      setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, TASK_INFO_PANEL_JS_QUERY_POOL_SIZE)
    }

    taskSpecificJBCefBrowser.jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, TASK_SPECIFIC_PANEL_JS_QUERY_POOL_SIZE)

    taskInfoJBCefBrowser.disableNavigation()
    Disposer.register(this, taskInfoJBCefBrowser)
    Disposer.register(this, taskSpecificJBCefBrowser)

    ApplicationManager.getApplication().messageBus.connect(this)
      .subscribe(
        LafManagerListener.TOPIC,
        LafManagerListener { TaskToolWindowView.updateAllTabs(project) })
  }

  override val taskInfoPanel: JComponent
    get() = taskInfoJBCefBrowser.component

  override val taskSpecificPanel: JComponent
    get() = taskSpecificJBCefBrowser.component

  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.JCEF

  override fun updateTaskInfoPanel(task: Task?) {
    taskInfoJBCefBrowser.component.isVisible = false

    val taskDescription = getTaskDescription(project, task, uiMode)

    taskInfoJBCefBrowser.loadHTML(taskDescription)
    taskInfoJBCefBrowser.component.isVisible = true
  }

  override fun updateTaskSpecificPanel(task: Task?) {
    taskSpecificJBCefBrowser.component.isVisible = false
  }

  override fun dispose() {
    super.dispose()
  }

  companion object {
    // maximum number of created qs queries in termsQueryManager
    private const val TASK_INFO_PANEL_JS_QUERY_POOL_SIZE = 3

    // maximum number of created qs queries in taskSpecificQueryManager
    private const val TASK_SPECIFIC_PANEL_JS_QUERY_POOL_SIZE = 2

    @TestOnly
    fun processContent(content: String, project: Project): String {
      return htmlWithResources(project, content)
    }
  }
}
