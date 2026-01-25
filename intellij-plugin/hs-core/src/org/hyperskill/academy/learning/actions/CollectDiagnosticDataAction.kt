package org.hyperskill.academy.learning.actions

import com.intellij.ide.actions.RevealFileAction
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.platform.IdeDetector
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class CollectDiagnosticDataAction : DumbAwareAction(
  EduCoreBundle.lazyMessage("action.collect.diagnostic.data.text")
) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val projectPath = project.basePath ?: return

    ProgressManager.getInstance().run(object : Task.Backgroundable(
      project,
      EduCoreBundle.message("action.collect.diagnostic.data.progress"),
      true
    ) {
      override fun run(indicator: ProgressIndicator) {
        try {
          indicator.isIndeterminate = false
          indicator.fraction = 0.0
          indicator.text = EduCoreBundle.message("action.collect.diagnostic.data.preparing")

          val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
          val projectName = project.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
          val archiveName = "hyperskill_diagnostic_${projectName}_$timestamp.zip"

          val desktopPath = FileUtil.toSystemIndependentName(System.getProperty("user.home")) + "/Desktop"
          val archiveFile = File(desktopPath, archiveName)

          ZipOutputStream(FileOutputStream(archiveFile)).use { zipOut ->
            // 1. Add diagnostic info file
            indicator.fraction = 0.1
            indicator.text = EduCoreBundle.message("action.collect.diagnostic.data.collecting.info")
            addDiagnosticInfo(zipOut, project)

            // 2. Add project files
            indicator.fraction = 0.2
            indicator.text = EduCoreBundle.message("action.collect.diagnostic.data.collecting.project")
            addProjectFiles(zipOut, projectPath, indicator)

            // 3. Add idea.log
            indicator.fraction = 0.9
            indicator.text = EduCoreBundle.message("action.collect.diagnostic.data.collecting.logs")
            addIdeaLog(zipOut)
          }

          indicator.fraction = 1.0

          // Show notification with link to open the archive location
          val revealActionName = RevealFileAction.getActionName()
          EduNotificationManager.create(
            NotificationType.INFORMATION,
            EduCoreBundle.message("action.collect.diagnostic.data.success.title"),
            EduCoreBundle.message("action.collect.diagnostic.data.success.message", archiveFile.absolutePath)
          ).addAction(NotificationAction.createSimple(revealActionName) {
            RevealFileAction.openFile(archiveFile)
          }).notify(project)

        } catch (ex: Exception) {
          LOG.error("Failed to collect diagnostic data", ex)
          EduNotificationManager.showErrorNotification(
            project,
            EduCoreBundle.message("action.collect.diagnostic.data.error.title"),
            EduCoreBundle.message("action.collect.diagnostic.data.error.message", ex.message ?: "Unknown error")
          )
        }
      }
    })
  }

  private fun addDiagnosticInfo(zipOut: ZipOutputStream, project: Project) {
    val info = buildString {
      appendLine("=== Hyperskill Academy Diagnostic Report ===")
      appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").format(Date())}")
      appendLine()

      appendLine("=== IDE Information ===")
      val appInfo = ApplicationInfo.getInstance()
      appendLine("IDE: ${appInfo.fullApplicationName}")
      appendLine("Build: ${appInfo.build.asString()}")
      appendLine("Product Code: ${IdeDetector.getProductCode()}")
      appendLine()

      appendLine("=== Plugin Information ===")
      val pluginId = PluginId.getId("org.hyperskill.academy")
      val plugin = PluginManagerCore.getPlugin(pluginId)
      appendLine("Plugin ID: $pluginId")
      appendLine("Plugin Version: ${plugin?.version ?: "Unknown"}")
      appendLine("Plugin Name: ${plugin?.name ?: "Unknown"}")
      appendLine()

      appendLine("=== Hyperskill Account ===")
      val account = HyperskillSettings.INSTANCE.account
      if (account != null) {
        val userInfo = account.userInfo
        appendLine("Logged In: Yes")
        appendLine("User ID: ${userInfo?.id ?: "Unknown"}")
        appendLine("User Name: ${userInfo?.getFullName() ?: "Unknown"}")
      } else {
        appendLine("Logged In: No")
      }
      appendLine()

      appendLine("=== System Information ===")
      appendLine("OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
      appendLine("Java Version: ${System.getProperty("java.version")}")
      appendLine("Java Runtime: ${System.getProperty("java.runtime.version")}")
      appendLine("Architecture: ${System.getProperty("os.arch")}")
      appendLine()

      appendLine("=== Project Information ===")
      appendLine("Project Name: ${project.name}")
      appendLine("Project Path: ${project.basePath}")
      appendLine()

      val course = StudyTaskManager.getInstance(project).course
      if (course != null) {
        appendLine("=== Course Information ===")
        appendCourseInfo(this, course)
      }
    }

    zipOut.putNextEntry(ZipEntry("diagnostic_info.txt"))
    zipOut.write(info.toByteArray(Charsets.UTF_8))
    zipOut.closeEntry()
  }

  private fun appendCourseInfo(sb: StringBuilder, course: Course) {
    with(sb) {
      appendLine("Course Name: ${course.name}")
      appendLine("Course Type: ${course.itemType}")
      appendLine("Language: ${course.languageId}")
      appendLine("Environment: ${course.environment}")

      if (course is HyperskillCourse) {
        appendLine("Hyperskill Project ID: ${course.hyperskillProject?.id}")
        appendLine("Hyperskill Project Title: ${course.hyperskillProject?.title}")
      }

      appendLine("Lessons: ${course.lessons.size}")
      course.lessons.forEachIndexed { index, lesson ->
        appendLine("  Lesson ${index + 1}: ${lesson.name} (${lesson.taskList.size} tasks)")
      }

      val currentTask = course.lessons.flatMap { it.taskList }.find { it.status.name != "Solved" }
      if (currentTask != null) {
        appendLine()
        appendLine("Current Task: ${currentTask.name}")
        appendLine("Task Status: ${currentTask.status}")
      }
    }
  }

  private fun addProjectFiles(zipOut: ZipOutputStream, projectPath: String, indicator: ProgressIndicator) {
    val projectDir = File(projectPath)
    val allFiles = collectProjectFiles(projectDir)
    val totalFiles = allFiles.size

    allFiles.forEachIndexed { index, file ->
      if (indicator.isCanceled) return

      val relativePath = file.relativeTo(projectDir).path
      indicator.text2 = relativePath
      indicator.fraction = 0.2 + (0.7 * index / totalFiles)

      try {
        zipOut.putNextEntry(ZipEntry("project/$relativePath"))
        Files.copy(file.toPath(), zipOut)
        zipOut.closeEntry()
      } catch (ex: Exception) {
        LOG.warn("Failed to add file to archive: $relativePath", ex)
      }
    }
  }

  private fun collectProjectFiles(projectDir: File): List<File> {
    return projectDir.walkTopDown()
      .filter { it.isFile }
      .toList()
  }

  private fun addIdeaLog(zipOut: ZipOutputStream) {
    val logPath = Path.of(PathManager.getLogPath(), "idea.log")
    if (Files.exists(logPath)) {
      try {
        zipOut.putNextEntry(ZipEntry("logs/idea.log"))
        Files.copy(logPath, zipOut)
        zipOut.closeEntry()
      } catch (ex: Exception) {
        LOG.warn("Failed to add idea.log to archive", ex)
      }
    }

    // Also try to add recent log file if exists
    val logDir = Path.of(PathManager.getLogPath())
    if (Files.exists(logDir)) {
      Files.list(logDir).use { stream ->
        stream
          .filter { it.fileName.toString().startsWith("idea.log.") }
          .sorted(Comparator.comparing<Path, java.nio.file.attribute.FileTime> { Files.getLastModifiedTime(it) }.reversed())
          .limit(1)
          .forEach { logFile ->
            try {
              zipOut.putNextEntry(ZipEntry("logs/${logFile.fileName}"))
              Files.copy(logFile, zipOut)
              zipOut.closeEntry()
            } catch (ex: Exception) {
              LOG.warn("Failed to add ${logFile.fileName} to archive", ex)
            }
          }
      }
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null && StudyTaskManager.getInstance(project).course != null
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    private val LOG = Logger.getInstance(CollectDiagnosticDataAction::class.java)
    const val ACTION_ID = "HyperskillEducational.CollectDiagnosticData"
  }
}
