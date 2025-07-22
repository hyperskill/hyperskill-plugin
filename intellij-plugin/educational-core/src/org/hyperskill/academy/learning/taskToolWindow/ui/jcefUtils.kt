package org.hyperskill.academy.learning.taskToolWindow.ui

import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import org.hyperskill.academy.learning.RemoteEnvHelper
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel.getIconPath
import org.hyperskill.academy.learning.taskToolWindow.links.JCefToolWindowLinkHandler
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager.INTELLIJ_ICON_QUICKFIX_OFF_BULB
import org.hyperskill.academy.learning.xmlEscaped
import org.jsoup.nodes.Element

class JCEFToolWindowRequestHandler(private val jcefLinkHandler: JCefToolWindowLinkHandler) : CefRequestHandlerAdapter() {
  /**
   * Called before browser navigation. If the navigation is canceled LoadError will be called with an ErrorCode value of Aborted.
   *
   * @return true to cancel the navigation or false to allow the navigation to proceed.
   */
  override fun onBeforeBrowse(
    browser: CefBrowser?,
    frame: CefFrame?,
    request: CefRequest?,
    user_gesture: Boolean,
    is_redirect: Boolean
  ): Boolean {
    val url = request?.url ?: return false
    val referUrl = request.referrerURL
    return jcefLinkHandler.process(url, referUrl)
  }
}

class JCEFTaskInfoLifeSpanHandler(private val jcefLinkHandler: JCefToolWindowLinkHandler) : CefLifeSpanHandlerAdapter() {
  override fun onBeforePopup(browser: CefBrowser?, frame: CefFrame?, targetUrl: String?, targetFrameName: String?): Boolean {
    if (targetUrl == null) return true
    return jcefLinkHandler.process(targetUrl)
  }
}

private const val HINT_HEADER: String = "hint_header"
private const val HINT_HEADER_EXPANDED: String = "$HINT_HEADER checked"
private const val HINT_BLOCK_TEMPLATE: String = """
                                          <div class="$HINT_HEADER">
                                            <img src="%s" style="display: inline-block;"> %s %s 
                                          </div>
                                          <div class="hint_content">
                                            %s
                                          </div>
                                          """
private const val HINT_EXPANDED_BLOCK_TEMPLATE: String = """
                                                   <div class="$HINT_HEADER_EXPANDED">
                                                     <img src="%s" style="display: inline-block;"> %s %s 
                                                   </div>
                                                   <div class='hint_content'>
                                                     %s
                                                   </div>
                                                   """

fun wrapHintJCEF(project: Project, hintElement: Element, displayedHintNumber: String, hintTitle: String): String {
  val course = StudyTaskManager.getInstance(project).course
  val hintText: String = hintElement.html()
  val escapedHintTitle = hintTitle.xmlEscaped
  if (course == null) {
    return String.format(HINT_BLOCK_TEMPLATE, escapedHintTitle, displayedHintNumber, hintText)
  }

  val bulbIcon = if (!RemoteEnvHelper.isRemoteDevServer()) {
    StyleResourcesManager.resourceUrl(INTELLIJ_ICON_QUICKFIX_OFF_BULB)
  }
  else {
    "https://intellij-icons.jetbrains.design/icons/AllIcons/expui/codeInsight/quickfixOffBulb.svg"
  }
  val bulbIconWithTheme = if (!isUnitTestMode) getIconPath(bulbIcon) else ""

  val study = course.isStudy
  return if (study) {
    String.format(HINT_BLOCK_TEMPLATE, bulbIconWithTheme, escapedHintTitle, displayedHintNumber, hintText)
  }
  else {
    String.format(HINT_EXPANDED_BLOCK_TEMPLATE, bulbIconWithTheme, escapedHintTitle, displayedHintNumber, hintText)
  }
}
