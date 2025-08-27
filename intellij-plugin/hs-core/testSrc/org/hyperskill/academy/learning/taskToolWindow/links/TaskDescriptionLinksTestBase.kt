package org.hyperskill.academy.learning.taskToolWindow.links

import com.intellij.testFramework.PlatformTestUtil
import org.hyperskill.academy.learning.EduTestCase

abstract class TaskDescriptionLinksTestBase : EduTestCase() {

  protected fun openLink(link: String) {
    ToolWindowLinkHandler(project).process(link)
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
  }
}
