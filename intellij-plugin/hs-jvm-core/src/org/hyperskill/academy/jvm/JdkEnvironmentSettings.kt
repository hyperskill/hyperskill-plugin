package org.hyperskill.academy.jvm

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.JavaSdkVersionUtil
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.util.lang.JavaVersion
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse

const val JVM_LANGUAGE_LEVEL = "jvm_language_level"

// TODO(refactor this), this is a temporary solution
// All JVM-based Hyperskill courses require at least JDK version 23
val hyperskillJdkVersion: JavaSdkVersion = JavaSdkVersion.JDK_23

/**
 * Checks that [this] JDK is new enough for a Hyperskill course, i.e. is [hyperskillJdkVersion] or newer.
 *
 * Feature versions are compared instead of `JavaSdkVersion` values because the latter enum has no entry for a JDK
 * newer than the one the IDE knows about: [JavaSdkVersionUtil.getJavaSdkVersion] returns `null` for such a JDK,
 * which would make a perfectly suitable installation look unsupported.
 */
fun Sdk.isAtLeastHyperskillJdkVersion(): Boolean {
  val requiredFeatureVersion = JavaVersion.tryParse(hyperskillJdkVersion.description)?.feature
  val actualFeatureVersion = versionString?.let { JavaVersion.tryParse(it) }?.feature
  if (requiredFeatureVersion != null && actualFeatureVersion != null) {
    return actualFeatureVersion >= requiredFeatureVersion
  }
  return JavaSdkVersionUtil.getJavaSdkVersion(this)?.isAtLeast(hyperskillJdkVersion) == true
}

val Course.minJvmSdkVersion: ParsedJavaVersion
  get() = when (this) {
    is HyperskillCourse -> JavaVersionParseSuccess(hyperskillJdkVersion)
    else -> ParsedJavaVersion.fromStringLanguageLevel(environmentSettings[JVM_LANGUAGE_LEVEL])
  }

fun jvmEnvironmentSettings(project: Project): Map<String, String> = mapOf(
  JVM_LANGUAGE_LEVEL to LanguageLevelProjectExtension.getInstance(project).languageLevel.toString()
)
