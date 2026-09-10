package org.hyperskill.academy.java

import com.intellij.openapi.projectRoots.JavaSdkVersion
import org.hyperskill.academy.jvm.JavaVersionParseSuccess
import org.hyperskill.academy.jvm.JdkLanguageSettings
import org.hyperskill.academy.jvm.ParsedJavaVersion
import org.hyperskill.academy.learning.courseFormat.Course

open class JLanguageSettings : JdkLanguageSettings() {

  override fun requiredJdkVersion(course: Course): ParsedJavaVersion {
    val javaVersionDescription = course.languageVersion ?: return JavaVersionParseSuccess(DEFAULT_JAVA)
    return ParsedJavaVersion.fromJavaSdkDescriptionString(javaVersionDescription)
  }

  companion object {
    val DEFAULT_JAVA = JavaSdkVersion.JDK_1_8
  }
}
