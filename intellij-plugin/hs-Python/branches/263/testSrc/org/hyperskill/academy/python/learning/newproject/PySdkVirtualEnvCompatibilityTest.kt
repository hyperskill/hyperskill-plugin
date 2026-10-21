package org.hyperskill.academy.python.learning.newproject

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.python.sdk.PythonSdkAdditionalData

class PySdkVirtualEnvCompatibilityTest : LightPlatformTestCase() {
  fun testSdkKeepsAdditionalDataForVirtualEnvCreation() {
    val sdk = PySdkToCreateVirtualEnv.create("Python 3.14", "/usr/bin/python3.14", "3.14")

    val preparedSdk = prepareSdkForVirtualEnvCreation(sdk)

    assertSame(sdk, preparedSdk)
    assertInstanceOf(preparedSdk.sdkAdditionalData, PythonSdkAdditionalData::class.java)
  }
}
