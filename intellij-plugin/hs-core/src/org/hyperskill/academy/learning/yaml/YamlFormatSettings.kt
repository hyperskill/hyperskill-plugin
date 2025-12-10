package org.hyperskill.academy.learning.yaml

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.guessCourseDir
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.COURSE_CONFIG

object YamlFormatSettings {
  private val YAML_IS_EDU_PROJECT: Key<Boolean> = Key.create("EDU.yaml_is_edu_project")

  /**
   * Determines whether current project is a Hyperskill course project by checking
   * if course config file exists and has type "hyperskill".
   *
   * Non-Hyperskill course types (like "coursera", "pycharm", etc.) are handled by
   * other plugins (e.g., JetBrains Academy plugin) and should be ignored here.
   *
   * IMPORTANT: Avoids VFS access on EDT. On UI thread, returns cached value if present
   * or schedules background computation and returns false as a conservative default.
   */
  fun Project.isEduYamlProject(): Boolean {
    // Use cached value if available
    getUserData(YAML_IS_EDU_PROJECT)?.let { return it }

    val app = ApplicationManager.getApplication()
    return if (app.isDispatchThread) {
      // Don't touch VFS on EDT. Compute in background and cache for subsequent calls.
      app.executeOnPooledThread {
        val result = isHyperskillCourseProject()
        putUserData(YAML_IS_EDU_PROJECT, result)
        if (result) {
          // Trigger StudyTaskManager initialization to load course once detection is ready
          // Call on EDT to keep consistency with callers expecting UI-thread work
          app.invokeLater { StudyTaskManager.getInstance(this) }
        }
      }
      false
    }
    else {
      val result = isHyperskillCourseProject()
      putUserData(YAML_IS_EDU_PROJECT, result)
      result
    }
  }

  /**
   * Checks if the project contains a Hyperskill course by examining the course config file.
   * Returns true only for courses with type "hyperskill".
   */
  private fun Project.isHyperskillCourseProject(): Boolean {
    val courseConfig = guessCourseDir()?.findChild(COURSE_CONFIG) ?: return false
    return isHyperskillCourseConfig(courseConfig)
  }

  /**
   * Parses the course config file to check if it's a Hyperskill course (type: hyperskill).
   * Uses simple text parsing to avoid full YAML deserialization overhead.
   */
  private fun isHyperskillCourseConfig(configFile: VirtualFile): Boolean {
    return try {
      val content = VfsUtil.loadText(configFile)
      // Look for "type: hyperskill" or "type:hyperskill" at the beginning of a line
      val typeRegex = """(?m)^type:\s*hyperskill\s*$""".toRegex()
      typeRegex.containsMatchIn(content)
    }
    catch (e: Exception) {
      false
    }
  }

  // it is here because it's used in test and main code
  val YAML_TEST_PROJECT_READY = Key<Boolean>("EDU.yaml_test_project_ready")
  val YAML_TEST_THROW_EXCEPTION = Key<Boolean>("EDU.yaml_test_throw_exception")

  fun shouldCreateConfigFiles(project: Project): Boolean = !isUnitTestMode || project.getUserData(YAML_TEST_PROJECT_READY) == true
}
