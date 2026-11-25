package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.querySystemPythonPaths

class PyManuallySelectedInterpreterTest : LightPlatformTestCase() {
  private var createdSdk: Sdk? = null

  override fun tearDown() {
    try {
      createdSdk?.let(SdkConfigurationUtil::removeSdk)
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  /**
   * An interpreter picked with the browse button of the course wizard is a [PyDetectedSdk].
   * It used to be registered with `SdkConfigurationUtil.createAndAddSDK`, which gives it no [PythonSdkAdditionalData].
   * The Python plugin refuses to set up such an SDK, so the project was opened without an interpreter.
   */
  fun testManuallySelectedInterpreterBecomesSdkWithAdditionalData() {
    val future = ApplicationManager.getApplication().executeOnPooledThread<List<String>> { querySystemPythonPaths() }
    val interpreterPath = PlatformTestUtil.waitForFuture(future, 60_000).firstOrNull()
    if (interpreterPath == null) {
      println("No local Python interpreter, nothing to check")
      return
    }

    val sdk = PyCourseProjectGenerator.updateSdkIfNeeded(project, PyDetectedSdk(interpreterPath))
    createdSdk = sdk

    assertNotNull("No SDK for $interpreterPath", sdk)
    assertInstanceOf(sdk!!.sdkAdditionalData, PythonSdkAdditionalData::class.java)
  }
}
