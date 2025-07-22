package org.hyperskill.academy.learning.taskToolWindow.ui.jcefSpecificQueries

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.Alarm
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.taskToolWindow.TERM_CLASS
import org.hyperskill.academy.learning.taskToolWindow.ui.canShowTerms

/**
 * Manages tooltips that display definitions of terms.
 *
 * Adds a mouseover event listener to all the terms (displayed as span elements with a specific style).
 * When a user hovers over a term, a tooltip containing the definition of the term is displayed.
 * Also adds a scroll listener to close the tooltip when a user scrolls the page.
 */
// TODO: Implement an analogue for Swing
class TermsQueryManager private constructor(
  private val project: Project,
  private val task: Task,
  private val taskJBCefBrowser: JBCefBrowserBase
) : Disposable {
  private val jsQueryMouseOverListener = JBCefJSQuery.create(taskJBCefBrowser)
  private val jsQueryMouseOutListener = JBCefJSQuery.create(taskJBCefBrowser)
  private val jsQueryScrollListener = JBCefJSQuery.create(taskJBCefBrowser)
  private var gotItTooltip: Balloon? = null

  // TODO(use coroutines instead of alarm after moving to ai package)
  private val alarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

  private val termListenerLoadHandler = object : CefLoadHandlerAdapter() {

    override fun onLoadEnd(browser: CefBrowser?, frame: CefFrame?, httpStatusCode: Int) {
      super.onLoadEnd(browser, frame, httpStatusCode)

      browser?.mainFrame?.executeJavaScript(
        """
          let terms = document.getElementsByClassName('$TERM_CLASS');
          [].slice.call(terms).forEach(term => {
            term.addEventListener('mouseover', function (event) {
              let boundingRect = term.getBoundingClientRect();
              let data = { 
                term: term.innerText, 
                x: event.clientX, 
                y: event.clientY, 
                bottomOfTermRect: boundingRect.bottom,
                topOfTermRect: boundingRect.top
              };
              ${jsQueryMouseOverListener.inject("JSON.stringify(data)")}
            });
            term.addEventListener('mouseout', function (event) {
              let data = { 
                term: term.innerText, 
                x: event.clientX, 
                y: event.clientY
              };
              ${jsQueryMouseOutListener.inject("JSON.stringify(data)")}
            });
          });
          window.addEventListener('scroll', function() { 
             ${jsQueryScrollListener.inject("window.scrollY")}
          });
          """.trimIndent(), taskJBCefBrowser.cefBrowser.url, 0
      )
    }
  }

  init {
    jsQueryMouseOverListener.addHandler { data ->
      showDefinitionOfTerm(data)
      null
    }
    jsQueryMouseOutListener.addHandler { data ->
      disposeExistingTooltip()
      null
    }
    jsQueryScrollListener.addHandler {
      disposeExistingTooltip()
      null
    }
    Disposer.register(this) {
      Disposer.dispose(jsQueryMouseOverListener)
      Disposer.dispose(jsQueryMouseOutListener)
      Disposer.dispose(jsQueryScrollListener)
    }
    taskJBCefBrowser.jbCefClient.addLoadHandler(termListenerLoadHandler, taskJBCefBrowser.cefBrowser)
  }

  private fun showDefinitionOfTerm(data: String) {
    if (data.isBlank()) return
    disposeExistingTooltip()
    return
  }

  private fun disposeExistingTooltip() {
    gotItTooltip?.let { Disposer.dispose(it) }
    alarm.cancelAllRequests()
    gotItTooltip = null
  }

  override fun dispose() {
    disposeExistingTooltip()
    taskJBCefBrowser.jbCefClient.removeLoadHandler(termListenerLoadHandler, taskJBCefBrowser.cefBrowser)
  }

  companion object {
    @JvmStatic
    fun getTermsQueryManager(project: Project, task: Task?, taskJBCefBrowser: JBCefBrowserBase): TermsQueryManager? {
      if (task == null || !canShowTerms(project, task)) return null
      return TermsQueryManager(project, task, taskJBCefBrowser)
    }
  }
}
