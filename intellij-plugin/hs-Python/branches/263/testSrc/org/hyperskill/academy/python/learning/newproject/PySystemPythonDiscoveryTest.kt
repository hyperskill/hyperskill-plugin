package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.python.sdk.querySystemPythonPaths

class PySystemPythonDiscoveryTest : LightPlatformTestCase() {
  /**
   * Since 263 `SystemPythonService.findSystemPythons` has no default value for its `eelApi` parameter.
   * Calling it through the default-arguments bridge passed `null` and failed with an NPE,
   * so the course wizard detected no local interpreter at all and only offered to download Python.
   */
  fun testSystemPythonServiceIsQueriedWithoutErrors() {
    val future = ApplicationManager.getApplication().executeOnPooledThread<List<String>> { querySystemPythonPaths() }

    val paths = PlatformTestUtil.waitForFuture(future, 60_000)

    assertTrue("Unexpected interpreter paths: $paths", paths.all { it.isNotBlank() })
  }
}
