package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.getSdksToInstall
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult

/**
 * "Install Python" suggestions are shown when no Python interpreter is detected on the machine.
 * Such an SDK is not installed yet, so it has no home path, and since 262 an SDK cannot be created with a `null` one.
 */
class PySdkToInstallCompatibilityTest : LightPlatformTestCase() {

  fun testInstallableSdksCanBeCreated() {
    // The list is empty on platforms where the Python plugin has nothing to suggest, the point is that creation does not fail
    for (sdk in getSdksToInstall()) {
      assertNotNull("Installable SDK must have a non-null home path", sdk.homePath)
      assertNotNull("Installable SDK must keep its version", sdk.versionString)
      // Otherwise the Python plugin detects the flavor by the empty home path and fails
      assertInstanceOf(sdk.sdkAdditionalData, PythonSdkAdditionalData::class.java)
    }
  }

  fun testSdkWithoutHomePathIsValidatedByItsVersion() {
    val settings = PyLanguageSettings()
    settings.getSettings().sdk = ProjectJdkImpl("Python 3.13.1", PythonSdkType.getInstance(), "", "3.13.1")

    val course = HyperskillCourse("Python", "Python", "3.11")

    val result = settings.validate(course, null)

    assertInstanceOf(result, SettingsValidationResult.Ready::class.java)
    assertNull("Python 3.13.1 is applicable to a 3.11 course", (result as SettingsValidationResult.Ready).validationMessage)
  }
}
