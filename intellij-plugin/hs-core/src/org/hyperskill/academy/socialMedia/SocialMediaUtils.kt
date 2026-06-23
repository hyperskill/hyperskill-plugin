package org.hyperskill.academy.socialMedia

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.swing.Icon

object SocialMediaUtils {

  /**
   * Hyperskill courses page the achievement points to. UTM tags are part of the agreed link and must be kept as is.
   * Used both as the `url` parameter of the X share intent and embedded into the LinkedIn share text.
   */
  const val SHARE_URL: String =
    "https://hyperskill.org/courses?source=ide_share&utm_source=jetbrains&utm_medium=social&utm_campaign=ide_plugin"

  // Human-friendly link shown in the dialog body (without tracking parameters)
  private const val COURSES_DISPLAY_LINK: String = "hyperskill.org/courses"

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

  /** The achievement sentence without any link. Shown as the base of the dialog message and used as the X post text. */
  private fun achievementCore(solvedTask: Task): String {
    val course = solvedTask.course
    val projectName = (course as? HyperskillCourse)?.getProjectLesson()?.presentableName ?: course.presentableName
    return EduCoreBundle.message("social.media.hyperskill.achievement.message", projectName)
  }

  /** Text shown (and selectable) in the dialog body: the achievement plus the human-friendly courses link. */
  fun getDisplayMessage(solvedTask: Task): String =
    "${achievementCore(solvedTask)} ${EduCoreBundle.message("social.media.learn.more.at", COURSES_DISPLAY_LINK)}"

  /**
   * X share intent. The post text intentionally has no link because X renders the [SHARE_URL] from the `url` parameter.
   * https://twitter.com/intent/tweet?text=...&url=...
   */
  fun buildXShareUrl(solvedTask: Task): String {
    val text = achievementCore(solvedTask)
    return "https://twitter.com/intent/tweet?text=${encode(text)}&url=${encode(SHARE_URL)}"
  }

  /**
   * LinkedIn share intent. LinkedIn has no separate `url` parameter, so the tracked [SHARE_URL] is embedded into the text.
   * https://www.linkedin.com/feed/?shareActive=true&text=...
   */
  fun buildLinkedInShareUrl(solvedTask: Task): String {
    val text = "${achievementCore(solvedTask)} ${EduCoreBundle.message("social.media.learn.more.at", SHARE_URL)}"
    return "https://www.linkedin.com/feed/?shareActive=true&text=${encode(text)}"
  }

  fun loadAchievementImage(): Icon? = IconLoader.findIcon(ACHIEVEMENT_IMAGE_PATH, SocialMediaUtils::class.java.classLoader)

  private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")
}
