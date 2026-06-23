package org.hyperskill.academy.socialMedia

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import javax.swing.Icon

object SocialMediaUtils {

  /**
   * Marketing page opened by the "Learn more" button of the suggestion dialog.
   * UTM tags are part of the agreed link and must be kept as is.
   */
  const val LEARN_MORE_URL: String =
    "https://hyperskill.org/courses?utm_source=jetbrains&utm_medium=social&utm_campaign=ide_plugin"

  private const val ACHIEVEMENT_IMAGE_PATH = "/socialMedia/hyperskill/project_complete.png"

  /**
   * Defines the policy when the user is suggested to share the achievement.
   * The dialog is shown only once the whole Hyperskill project (the project lesson) is solved
   * and only right after the task transitions to the solved state (not on re-solving an already solved task).
   */
  fun shouldSuggestToPost(project: Project, solvedTask: Task, statusBeforeCheck: CheckStatus): Boolean {
    val course = project.course as? HyperskillCourse ?: return false
    if (!course.isStudy) return false
    if (statusBeforeCheck == CheckStatus.Solved) return false

    val projectLesson = course.getProjectLesson() ?: return false
    if (solvedTask.lesson != projectLesson) return false

    var allProjectTasksSolved = true
    projectLesson.visitTasks {
      allProjectTasksSolved = allProjectTasksSolved && it.status == CheckStatus.Solved
    }
    return allProjectTasksSolved
  }

  fun getMessage(solvedTask: Task): String {
    val course = solvedTask.course
    val projectName = (course as? HyperskillCourse)?.getProjectLesson()?.presentableName ?: course.presentableName
    return EduCoreBundle.message("social.media.hyperskill.achievement.message", projectName)
  }

  fun loadAchievementImage(): Icon? = IconLoader.findIcon(ACHIEVEMENT_IMAGE_PATH, SocialMediaUtils::class.java.classLoader)
}
