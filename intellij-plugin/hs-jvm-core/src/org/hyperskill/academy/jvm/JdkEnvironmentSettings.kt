package org.hyperskill.academy.jvm

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.util.lang.JavaVersion
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

const val JVM_LANGUAGE_LEVEL = "jvm_language_level"

// TODO(refactor this), this is a temporary solution
// Every JVM-based Hyperskill course is built and checked with JDK 23
val hyperskillJdkVersion: JavaSdkVersion = JavaSdkVersion.JDK_23

/**
 * Feature version of [this] JDK version, i.e. 23 for [JavaSdkVersion.JDK_23] and 8 for [JavaSdkVersion.JDK_1_8],
 * or `null` if it cannot be determined.
 */
val JavaSdkVersion.featureVersion: Int?
  get() = JavaVersion.tryParse(description)?.feature

/**
 * Feature version of a JDK reporting [versionString], or `null` when the string cannot be parsed or names a
 * pre-release build (`26-ea`, `25-internal`, `23-valhalla`, ...).
 *
 * A pre-release build deliberately counts as having no version at all: the Gradle integration refuses to run on one
 * and silently falls back to an arbitrary installation instead, so such a JDK must never end up on a course.
 *
 * Feature numbers are used instead of [JavaSdkVersion] values because that enum has no entry for a JDK newer than the
 * one the IDE was built with and reports `null` for it, which used to make a perfectly good installation look
 * unusable.
 */
fun releaseFeatureVersion(versionString: String?): Int? {
  val version = versionString ?: return null
  if (PRE_RELEASE_JDK_VERSION.containsMatchIn(version)) return null
  val javaVersion = JavaVersion.tryParse(version) ?: return null
  return if (javaVersion.ea) null else javaVersion.feature
}

val Sdk.releaseFeatureVersion: Int?
  get() = releaseFeatureVersion(versionString)

/** Matches a feature version followed by a pre-release qualifier: `26-ea`, `25-internal`, `23-valhalla`. */
private val PRE_RELEASE_JDK_VERSION = Regex("""\d+(\.\d+)*-[A-Za-z]""")

/**
 * Whether [this] JDK is really installed, i.e. whether its home directory holds a java launcher.
 *
 * [com.intellij.openapi.projectRoots.ProjectJdkTable] keeps an entry after its JDK has been uninstalled: the IDE only
 * paints it red in the JDK combo box. Such an entry still reports the version string it was registered with, so every
 * version check accepts it, and the learner lands on a project whose Gradle sync fails with
 * `Invalid Gradle JDK configuration found`.
 *
 * An existing directory is not enough either. `JdkInstaller.prepareJdkInstallation` creates the java home *before* the
 * first byte is downloaded, so a download that is still running, was cancelled or failed would otherwise pass for an
 * installed JDK -- and the course dialog would start a course on an empty folder.
 */
val Sdk.hasExistingHome: Boolean
  get() {
    val homePath = homePath ?: return false
    return try {
      val home = Path.of(homePath)
      Files.isRegularFile(home.resolve(UNIX_JAVA_LAUNCHER)) || Files.isRegularFile(home.resolve(WINDOWS_JAVA_LAUNCHER))
    }
    catch (_: InvalidPathException) {
      false
    }
  }

private const val UNIX_JAVA_LAUNCHER = "bin/java"
private const val WINDOWS_JAVA_LAUNCHER = "bin/java.exe"

/**
 * Whether [this] JDK is the one every JVM Hyperskill course is built and checked with.
 *
 * The version is pinned, not a lower bound: the generated Gradle scripts derive the Java toolchain from the JDK the
 * daemon runs on, so a newer JDK quietly changes what the learner's code is compiled against, and Hyperskill's own
 * tests run on [hyperskillJdkVersion].
 */
fun Sdk.isHyperskillJdkVersion(): Boolean {
  val featureVersion = releaseFeatureVersion ?: return false
  return featureVersion == hyperskillJdkVersion.featureVersion
}

/** The JDK version a course has to be opened with. */
val Course.requiredJdkVersion: ParsedJavaVersion
  get() = when (this) {
    is HyperskillCourse -> JavaVersionParseSuccess(hyperskillJdkVersion)
    else -> ParsedJavaVersion.fromStringLanguageLevel(environmentSettings[JVM_LANGUAGE_LEVEL])
  }

fun jvmEnvironmentSettings(project: Project): Map<String, String> = mapOf(
  JVM_LANGUAGE_LEVEL to LanguageLevelProjectExtension.getInstance(project).languageLevel.toString()
)
