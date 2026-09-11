package org.hyperskill.academy.sql.jvm.gradle

import org.hyperskill.academy.jvm.JdkLanguageSettings
import org.hyperskill.academy.jvm.JdkProjectSettings

class SqlJdkLanguageSettings : JdkLanguageSettings() {

  private var testLanguage: SqlTestLanguage? = null

  override fun getSettings(): JdkProjectSettings = SqlJdkProjectSettings(sdkModel, jdk, testLanguage)
}
