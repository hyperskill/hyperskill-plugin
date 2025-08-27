package org.hyperskill.academy.sql.core

import icons.DatabaseIcons
import org.hyperskill.academy.learning.EduExperimentalFeatures
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.isFeatureEnabled
import org.hyperskill.academy.learning.newproject.EduProjectSettings
import javax.swing.Icon

interface SqlConfiguratorBase<Settings : EduProjectSettings> : EduConfigurator<Settings> {
  override val logo: Icon
    get() = DatabaseIcons.Sql

  override val isEnabled: Boolean
    get() = isFeatureEnabled(EduExperimentalFeatures.SQL_COURSES)

  companion object {
    const val TASK_SQL: String = "task.sql"
  }
}
