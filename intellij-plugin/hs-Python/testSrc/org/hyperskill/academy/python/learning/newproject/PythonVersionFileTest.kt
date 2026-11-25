package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.python.sdk.PythonSdkType
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessageType

class PythonVersionFileTest : LightPlatformTestCase() {

  fun testMinorVersion() = assertParsed("3.12", PythonVersionRequirement(3, 12))

  fun testPatchVersionIsIgnored() = assertParsed("3.12.4\n", PythonVersionRequirement(3, 12))

  fun testFirstLineThatIsNotBlankOrComment() = assertParsed("# pinned for the course\n\n  3.11  \n3.12\n", PythonVersionRequirement(3, 11))

  fun testUvRequests() {
    assertParsed("cpython@3.13", PythonVersionRequirement(3, 13))
    assertParsed("cpython-3.12.4-macos-aarch64-none", PythonVersionRequirement(3, 12))
    assertParsed("3.13t", PythonVersionRequirement(3, 13))
  }

  fun testUnsupportedRequests() {
    for (text in listOf("", "\n# comment only\n", "3", "system", "pypy@3.10", "pypy3.10-7.3.12", ">=3.11")) {
      assertNull("`$text` must not be parsed", PythonVersionRequirement.parse(text))
    }
  }

  fun testRequirementIsTakenFromCourseRoot() {
    assertEquals(PythonVersionRequirement(3, 12), pinnedCourse("3.12\n").pythonVersionRequirement)
    assertNull(course().pythonVersionRequirement)

    val courseWithNestedFile = course().apply { additionalFiles = listOf(EduFile("src/$PYTHON_VERSION_FILE", "3.12")) }
    assertNull(courseWithNestedFile.pythonVersionRequirement)
  }

  fun testMatchingLocalInterpretersAreOffered() {
    val python312 = localSdk("3.12")
    val sdks = PyLanguageSettings().pinnedPySdks(
      PythonVersionRequirement(3, 12),
      listOf(localSdk("3.13"), python312),
      listOf(sdkToInstall("3.11.9"))
    )
    assertEquals(listOf(python312), sdks)
  }

  fun testMatchingInterpreterToInstallIsOfferedIfNoneIsInstalled() {
    val python311 = sdkToInstall("3.11.9")
    val sdks = PyLanguageSettings().pinnedPySdks(
      PythonVersionRequirement(3, 11),
      listOf(localSdk("3.13"), localSdk("3.12")),
      listOf(sdkToInstall("3.10.11"), python311)
    )
    assertEquals(listOf(python311), sdks)
  }

  fun testNothingIsOfferedIfPinnedVersionIsUnavailable() {
    val sdks = PyLanguageSettings().pinnedPySdks(
      PythonVersionRequirement(3, 9),
      listOf(localSdk("3.13")),
      emptyList()
    )
    assertNull(sdks)
  }

  fun testPinnedVersionIsValid() {
    val result = validate(pinnedCourse("3.12"), sdkWithVersion("3.12.1"))

    assertInstanceOf(result, SettingsValidationResult.Ready::class.java)
    assertNull((result as SettingsValidationResult.Ready).validationMessage)
  }

  fun testOtherVersionIsAllowedWithWarning() {
    val result = validate(pinnedCourse("3.12"), sdkWithVersion("3.13.1"))

    assertInstanceOf(result, SettingsValidationResult.ReadyWithWarning::class.java)
    val validationMessage = (result as SettingsValidationResult.ReadyWithWarning).validationMessage
    assertEquals(ValidationMessageType.WARNING, validationMessage.type)
    assertTrue(validationMessage.message, "Python 3.12" in validationMessage.message)
  }

  private fun assertParsed(text: String, expected: PythonVersionRequirement) {
    assertEquals(expected, PythonVersionRequirement.parse(text))
  }

  private fun course(): Course = HyperskillCourse("Python", "Python", PYTHON_3_VERSION)

  private fun pinnedCourse(version: String): Course = course().apply {
    additionalFiles = listOf(EduFile(PYTHON_VERSION_FILE, version))
  }

  private fun validate(course: Course, sdk: Sdk): SettingsValidationResult {
    val settings = PyLanguageSettings()
    settings.getSettings().sdk = sdk
    return settings.validate(course, null)
  }

  private fun localSdk(version: String): Sdk = PySdkToCreateVirtualEnv.create("Python $version", "/python$version/bin/python", version)

  // "Install Python" suggestions have no home path, so their language level comes from the version string
  private fun sdkToInstall(version: String): Sdk = sdkWithVersion(version)

  private fun sdkWithVersion(version: String): Sdk = ProjectJdkImpl("Python $version", PythonSdkType.getInstance(), "", version)
}
