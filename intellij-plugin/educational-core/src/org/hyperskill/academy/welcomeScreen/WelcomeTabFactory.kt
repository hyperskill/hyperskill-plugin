package org.hyperskill.academy.welcomeScreen

import com.intellij.openapi.Disposable
import com.intellij.openapi.wm.WelcomeScreen
import com.intellij.openapi.wm.WelcomeScreenTab
import com.intellij.openapi.wm.WelcomeTabFactory

class HyperskillWelcomeTabFactory : WelcomeTabFactory {
  override fun createWelcomeTabs(ws: WelcomeScreen, parentDisposable: Disposable): List<WelcomeScreenTab> {
    return listOf(WelcomeScreenTab())
  }
}
