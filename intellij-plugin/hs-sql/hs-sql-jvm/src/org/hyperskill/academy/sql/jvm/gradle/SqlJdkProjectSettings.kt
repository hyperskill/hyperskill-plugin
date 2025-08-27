package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import org.hyperskill.academy.jvm.JdkProjectSettings

class SqlJdkProjectSettings(
  model: ProjectSdksModel,
  jdk: Sdk?,
  val testLanguage: SqlTestLanguage?
) : JdkProjectSettings(model, jdk)
