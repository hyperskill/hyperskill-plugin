package org.hyperskill.academy.java.hyperskill

import org.hyperskill.academy.java.JLanguageSettings
import org.hyperskill.academy.jvm.JavaVersionParseSuccess
import org.hyperskill.academy.jvm.ParsedJavaVersion
import org.hyperskill.academy.jvm.hyperskillJdkVersion
import org.hyperskill.academy.learning.courseFormat.Course

class JHyperskillLanguageSettings : JLanguageSettings() {
  override fun minJvmSdkVersion(course: Course): ParsedJavaVersion {
    return JavaVersionParseSuccess(hyperskillJdkVersion)
  }
}