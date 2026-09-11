package org.hyperskill.academy.learning.taskToolWindow.ui

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefClient
import com.intellij.ui.jcef.JCEFHtmlPanel
import org.hyperskill.academy.learning.JavaUILibrary
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
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

    // Both browsers are created with a `null` URL, which CEF resolves to a non-existent `file:///jbcefbrowser/...`
    // page. Until something loads content into them that Chromium error page is what the user sees, so put the
    // regular "open any task" placeholder there right away.
    taskInfoJBCefBrowser.loadHTML(getTaskDescription(project, null, uiMode))
    taskSpecificJBCefBrowser.loadHTML(EMPTY_HTML)

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
    // The panel is deliberately not hidden first: if building the description throws, an invisible panel would
    // never come back, and the browser would keep showing the page it was constructed with.
    val taskDescription = try {
      getTaskDescription(project, task, uiMode)
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Exception) {
      LOG.warn("Failed to build the description of the task `${task?.name}`", e)
      EduCoreBundle.message("task.description.not.found")
    }

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
    private val LOG = Logger.getInstance(JCEFToolWindow::class.java)

    private const val EMPTY_HTML = "<html><body></body></html>"

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
