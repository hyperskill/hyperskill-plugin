package org.hyperskill.academy.jvm

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path

/**
 * Covers which JDK is offered to a learner for a course that requires a particular Java version.
 *
 * A course pins its JDK instead of setting a lower bound, so "newer" is as unusable here as "older".
 */
class JdkSelectionTest : BasePlatformTestCase() {

  private lateinit var jdkHomes: Path

  override fun setUp() {
    super.setUp()
    jdkHomes = FileUtil.createTempDirectory("jdk-selection-test", null, true).toPath()
  }

  fun `test the required version is offered`() {
    val model = modelOf("17.0.9", "25.0.1", "23.0.2", "21.0.5")

    assertSelected("23.0.2", JdkLanguageSettings.findSuitableJdk(required(JavaSdkVersion.JDK_23), model))
  }

  fun `test a newer jdk is not offered instead of the required one`() {
    // The generated Gradle scripts derive the Java toolchain from the JDK the daemon runs on, so a newer JDK compiles
    // the learner's code against something the course was not written for
    val model = modelOf("24.0.2", "25.0.1", "26.0.1")

    assertNull(JdkLanguageSettings.findSuitableJdk(required(JavaSdkVersion.JDK_23), model))
  }

  fun `test outdated jdks are not offered`() {
    val model = modelOf("17.0.9", "21.0.5", "11.0.22")

    assertNull(JdkLanguageSettings.findSuitableJdk(required(JavaSdkVersion.JDK_23), model))
  }

  fun `test pre-release builds of the required version are not offered`() {
    // The Gradle integration refuses to run on a pre-release JDK
    val model = modelOf("23-ea", "23-valhalla", "23.0.2")

    assertSelected("23.0.2", JdkLanguageSettings.findSuitableJdk(required(JavaSdkVersion.JDK_23), model))
  }

  fun `test nothing is offered when only pre-release builds of the required version are installed`() {
    val model = modelOf("23-ea", "23-internal")

    assertNull(JdkLanguageSettings.findSuitableJdk(required(JavaSdkVersion.JDK_23), model))
  }

  fun `test any jdk is offered when the course requires no particular version`() {
    // Which one is picked is not defined: `ProjectSdksModel` does not keep its SDKs in any particular order
    val model = modelOf("17.0.9", "11.0.22")

    assertNotNull(JdkLanguageSettings.findSuitableJdk(JavaVersionNotProvided, model))
  }

  fun `test uninstalled jdks are not offered`() {
    // The learner deleted JDK 23 from disk: its entry survives in the table and still reports version 23.0.2,
    // but the project it produces cannot be built
    val installed = jdk("23.0.2")
    val model = ProjectSdksModel().apply {
      addSdk(uninstalledJdk("23.0.2"))
      addSdk(installed)
    }

    assertEquals(installed.homePath, JdkLanguageSettings.findSuitableJdk(required(JavaSdkVersion.JDK_23), model)?.homePath)
  }

  fun `test nothing is offered when the required jdk was uninstalled`() {
    val model = ProjectSdksModel().apply {
      addSdk(uninstalledJdk("23.0.2"))
      addSdk(jdk("21.0.5"))
    }

    assertNull(JdkLanguageSettings.findSuitableJdk(required(JavaSdkVersion.JDK_23), model))
  }

  fun `test a jdk that is still being downloaded is not offered`() {
    // `JdkInstaller.prepareJdkInstallation` creates the java home before the download starts, so an existing directory
    // is not proof of an installed JDK
    val model = ProjectSdksModel().apply { addSdk(jdkBeingDownloaded("23.0.2")) }

    assertNull(JdkLanguageSettings.findSuitableJdk(required(JavaSdkVersion.JDK_23), model))
  }

  fun `test uninstalled jdk is not offered when the course requires no particular version`() {
    val model = ProjectSdksModel().apply { addSdk(uninstalledJdk("17.0.9")) }

    assertNull(JdkLanguageSettings.findSuitableJdk(JavaVersionNotProvided, model))
  }

  fun `test only the required version is suitable`() {
    val required = required(JavaSdkVersion.JDK_23)

    assertTrue(JdkLanguageSettings.isSuitableJdk(jdk("23.0.2"), required))

    assertFalse(JdkLanguageSettings.isSuitableJdk(jdk("25.0.1"), required))
    assertFalse(JdkLanguageSettings.isSuitableJdk(jdk("21.0.5"), required))
    assertFalse(JdkLanguageSettings.isSuitableJdk(jdk("1.8.0_402"), required))
    // A pre-release build of the required version is not the required version either
    assertFalse(JdkLanguageSettings.isSuitableJdk(jdk("23-ea"), required))
    assertFalse(JdkLanguageSettings.isSuitableJdk(null, required))
  }

  fun `test uninstalled jdk is not suitable even when the learner picked it explicitly`() {
    // Otherwise the course starts on a JDK that is not there any more instead of downloading the required one
    assertFalse(JdkLanguageSettings.isSuitableJdk(uninstalledJdk("23.0.2"), required(JavaSdkVersion.JDK_23)))
    assertFalse(JdkLanguageSettings.isSuitableJdk(uninstalledJdk("23.0.2"), JavaVersionNotProvided))
  }

  fun `test version of a jdk newer than the ide knows about is still parsed`() {
    // `JavaSdkVersion` has no entry past the JDK the IDE was built with, so the check must not rely on it
    val newerThanTheEnum = JavaSdkVersion.entries.last().featureVersion!! + 1

    assertEquals(newerThanTheEnum, releaseFeatureVersion("$newerThanTheEnum.0.1"))
    assertNull(releaseFeatureVersion("$newerThanTheEnum-ea"))
    assertNull(releaseFeatureVersion(null))
  }

  private fun assertSelected(expectedVersion: String, actual: Sdk?) {
    assertEquals(expectedVersion, actual?.versionString)
  }

  private fun required(version: JavaSdkVersion): ParsedJavaVersion = JavaVersionParseSuccess(version)

  /** A JDK that is installed, i.e. whose home directory holds a java launcher. */
  private fun jdk(versionString: String): Sdk = sdk(versionString, javaHome("jdk-$versionString", withLauncher = true))

  /** A JDK the learner uninstalled: the entry is still there, its home directory is not. */
  private fun uninstalledJdk(versionString: String): Sdk = sdk(versionString, jdkHomes.resolve("uninstalled-$versionString"))

  /**
   * A JDK whose home directory exists but is empty, the way `JdkInstaller.prepareJdkInstallation` leaves it before the
   * first byte is downloaded.
   */
  private fun jdkBeingDownloaded(versionString: String): Sdk =
    sdk(versionString, javaHome("downloading-$versionString", withLauncher = false))

  private fun javaHome(name: String, withLauncher: Boolean): Path {
    val home = jdkHomes.resolve(name)
    FileUtil.createDirectory(home.toFile())
    if (withLauncher) {
      FileUtil.createIfDoesntExist(home.resolve("bin").resolve(if (SystemInfo.isWindows) "java.exe" else "java").toFile())
    }
    return home
  }

  private fun sdk(versionString: String, home: Path): Sdk =
    ProjectJdkImpl("JDK $versionString", JavaSdk.getInstance(), home.toString(), versionString)

  private fun modelOf(vararg versionStrings: String): ProjectSdksModel = ProjectSdksModel().apply {
    versionStrings.forEach { addSdk(jdk(it)) }
  }
}
