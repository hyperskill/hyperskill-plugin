package org.hyperskill.academy.learning.actions.navigate

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import org.hyperskill.academy.learning.EduTestCase

abstract class NavigationTestBase : EduTestCase() {
  protected val rootDir: VirtualFile get() = LightPlatformTestCase.getSourceRoot()
}