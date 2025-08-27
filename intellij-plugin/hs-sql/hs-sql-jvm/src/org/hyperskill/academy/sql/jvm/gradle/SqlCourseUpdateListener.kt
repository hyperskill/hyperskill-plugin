package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.CourseUpdateListener
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.allTasks
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.sql.core.SqlConfiguratorBase

class SqlCourseUpdateListener : CourseUpdateListener {
  override fun courseUpdated(project: Project, course: Course) {
    if (course.configurator !is SqlConfiguratorBase) return

    val dataSourceManager = LocalDataSourceManager.getInstance(project)

    for (dataSource in dataSourceManager.dataSources) {
      if (dataSource.isTaskDataSource() && dataSource.task(project) == null) {
        dataSourceManager.removeDataSource(dataSource)
      }
    }

    val dataSourceUrls = dataSourceManager.dataSources.mapNotNullTo(HashSet()) { it.url }

    val tasksWithoutDataSource = course.allTasks.filter {
      it.databaseUrl(project) !in dataSourceUrls
    }

    createDataSources(project, tasksWithoutDataSource)
    attachSqlConsoleForOpenFiles(project)
    executeInitScripts(project, tasksWithoutDataSource)
  }
}
