package org.hyperskill.academy.welcomeScreen

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.wm.impl.welcomeScreen.TabbedWelcomeScreen.DefaultWelcomeScreenTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hyperskill.academy.learning.newproject.ui.CoursesPanelWithTabs
import javax.swing.JComponent
import kotlin.coroutines.CoroutineContext

class WelcomeScreenTab : DefaultWelcomeScreenTab("Hyperskill Academy"), CoroutineScope, Disposable {
  private val job = SupervisorJob()

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main + ModalityState.any().asContextElement()

  override fun buildComponent(): JComponent {
    val panel = CoursesPanelWithTabs(this, this)
    this.launch {
      panel.loadCourses()
    }
    return panel
  }

  override fun dispose() {
    job.cancel()
  }
}
