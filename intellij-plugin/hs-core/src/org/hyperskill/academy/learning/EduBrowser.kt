package org.hyperskill.academy.learning

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import java.net.URL

open class EduBrowser : EduTestAware {
  fun browse(url: URL) = browse(url.toExternalForm())

  open fun browse(link: String) {
    BrowserUtil.browse(link)
  }

  companion object {
    fun getInstance(): EduBrowser = service()
  }
}