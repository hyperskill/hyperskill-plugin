package org.hyperskill.academy.java

import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import org.hyperskill.academy.jvm.JavaVersionParseSuccess
import org.hyperskill.academy.jvm.JdkLanguageSettings
import org.hyperskill.academy.jvm.ParsedJavaVersion
import org.hyperskill.academy.learning.courseFormat.Course

open class JLanguageSettings : JdkLanguageSettings() {

  // Note: setupProjectSdksModel is intentionally not overridden here.
  // Adding SDK via model.addSdk() on EDT is prohibited in IntelliJ 2025.3+.
  // Bundled JDK is added in addBundledJdkIfNeeded() which is called from background thread.

  override fun addBundledJdkIfNeeded(model: ProjectSdksModel) {
    val (jdkPath, sdk) = findBundledJdk(model) ?: return
    if (sdk == null) {
      model.addSdk(JavaSdk.getInstance(), jdkPath, null)
    }
  }

  override fun minJvmSdkVersion(course: Course): ParsedJavaVersion {
    val javaVersionDescription = course.languageVersion ?: return JavaVersionParseSuccess(DEFAULT_JAVA)
    return ParsedJavaVersion.fromJavaSdkDescriptionString(javaVersionDescription)
  }

  companion object {
    val DEFAULT_JAVA = JavaSdkVersion.JDK_1_8
  }
}
