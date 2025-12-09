package org.hyperskill.academy.welcomeScreen

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.impl.welcomeScreen.TabbedWelcomeScreen.DefaultWelcomeScreenTab
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.hyperskill.academy.learning.newproject.ui.CoursesPanelWithTabs
import javax.swing.JComponent
import kotlin.coroutines.CoroutineContext

class WelcomeScreenTab(parentDisposable: Disposable) : DefaultWelcomeScreenTab("Hyperskill Academy"), CoroutineScope {
  private val job = SupervisorJob()
  private val disposable: Disposable = Disposer.newDisposable(parentDisposable, "WelcomeScreenTab")

  init {
    Disposer.register(disposable) {
      job.cancel()
    }
  }

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main + ModalityState.any().asContextElement()

  override fun buildComponent(): JComponent {
    val panel = CoursesPanelWithTabs(this, disposable, isPreferredSize = false)
    this.launch {
      panel.loadCourses()
    }
    return panel
  }
}
