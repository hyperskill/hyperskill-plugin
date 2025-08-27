package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.database.dataSource.LocalDataSourceManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import org.hyperskill.academy.learning.actions.RevertTaskAction
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.sql.core.SqlConfiguratorBase
import java.io.IOException

class SqlRevertTaskExtension : RevertTaskAction.RevertTaskExtension {
  override fun onTaskReversion(project: Project, task: Task) {
    if (task.course.configurator !is SqlConfiguratorBase) return

    val dataSource = task.findDataSource(project) ?: return
    LocalDataSourceManager.getInstance(project).removeDataSource(dataSource)

    dropDatabaseState(task, project)

    val tasks = listOf(task)
    createDataSources(project, tasks)
    attachSqlConsoleForOpenFiles(project, task)
    executeInitScripts(project, tasks)
  }

  // Dependency on concrete database kind/SQL dialect
  // since this code knows how and where database files are stored
  private fun dropDatabaseState(task: Task, project: Project) {
    val taskDir = task.getBaseTaskDir(project) ?: return
    // Refresh is important here since db directory is created by an external process
    // Heavily depends on the implementation of `Task.databaseUrl`
    val dbDir = LocalFileSystem.getInstance().refreshAndFindFileByPath("${taskDir.path}/db")
    if (dbDir != null) {
      try {
        runWriteAction {
          dbDir.delete(SqlRevertTaskExtension::class.java)
        }
      }
      catch (e: IOException) {
        LOG.error(e)
      }
    }
  }

  companion object {
    private val LOG = logger<SqlRevertTaskExtension>()
  }
}
