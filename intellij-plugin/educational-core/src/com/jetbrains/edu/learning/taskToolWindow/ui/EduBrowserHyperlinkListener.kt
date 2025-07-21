package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ui.BrowserHyperlinkListener
import javax.swing.event.HyperlinkEvent

open class EduBrowserHyperlinkListener : BrowserHyperlinkListener() {
  override fun hyperlinkActivated(e: HyperlinkEvent) {
    super.hyperlinkActivated(e)
  }

  companion object {
    val INSTANCE: EduBrowserHyperlinkListener = EduBrowserHyperlinkListener()
  }
}