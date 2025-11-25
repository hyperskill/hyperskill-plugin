package org.hyperskill.academy.learning.yaml

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.guessCourseDir
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.COURSE_CONFIG

object YamlFormatSettings {
  private val YAML_IS_EDU_PROJECT: Key<Boolean> = Key.create("EDU.yaml_is_edu_project")

  /**
   * Determines whether current project looks like an Edu YAML project by checking
   * if course config file exists in the project (course) root.
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
        val result = guessCourseDir()?.findChild(COURSE_CONFIG) != null
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
      val result = guessCourseDir()?.findChild(COURSE_CONFIG) != null
      putUserData(YAML_IS_EDU_PROJECT, result)
      result
    }
  }

  // it is here because it's used in test and main code
  val YAML_TEST_PROJECT_READY = Key<Boolean>("EDU.yaml_test_project_ready")
  val YAML_TEST_THROW_EXCEPTION = Key<Boolean>("EDU.yaml_test_throw_exception")

  fun shouldCreateConfigFiles(project: Project): Boolean = !isUnitTestMode || project.getUserData(YAML_TEST_PROJECT_READY) == true
}
