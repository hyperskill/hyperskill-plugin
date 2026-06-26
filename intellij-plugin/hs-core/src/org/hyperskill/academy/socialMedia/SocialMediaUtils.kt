package org.hyperskill.academy.socialMedia

import com.intellij.openapi.project.Project
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO
import javax.swing.Icon
import kotlin.math.roundToInt

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
   * X share intent. The post text includes the "Learn more at ..." part (the same text shown in the dialog),
   * and the tracked [SHARE_URL] is additionally passed via the `url` parameter.
   * https://twitter.com/intent/tweet?text=...&url=...
   */
  fun buildXShareUrl(solvedTask: Task): String {
    val text = getDisplayMessage(solvedTask)
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

  /**
   * Loads the full-resolution banner and returns a DPI-aware icon that downscales it to the exact device size
   * at paint time. This keeps the source pixels intact (no pre-baked downscale) so it stays crisp at any DPI.
   */
  fun loadAchievementImage(): Icon? {
    val source = runCatching {
      SocialMediaUtils::class.java.getResourceAsStream(ACHIEVEMENT_IMAGE_PATH)?.use { ImageIO.read(it) }
    }.getOrNull() ?: return null
    return ScaledBannerIcon(source, JBUI.scale(BANNER_WIDTH))
  }

  private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20")

  // Logical width of the banner in the dialog (independent of monitor DPI; user-scaled via JBUI.scale)
  private const val BANNER_WIDTH = 600
}

/**
 * An [Icon] that holds the full-resolution banner and downscales it to the exact device-pixel size on paint,
 * using a high-quality multi-step scale. The result is cached per device size, so repaints are cheap.
 */
private class ScaledBannerIcon(private val source: BufferedImage, private val logicalWidth: Int) : Icon {

  private val logicalHeight: Int = (logicalWidth.toDouble() * source.height / source.width).roundToInt()

  private var cached: BufferedImage? = null
  private var cachedWidth = -1
  private var cachedHeight = -1

  override fun getIconWidth(): Int = logicalWidth
  override fun getIconHeight(): Int = logicalHeight

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as Graphics2D
    try {
      // The monitor (system) scale lives in the graphics transform, so compute the real device size to scale to
      val sysScale = JBUIScale.sysScale(g2)
      val deviceWidth = maxOf(1, (logicalWidth * sysScale).roundToInt())
      val deviceHeight = maxOf(1, (logicalHeight * sysScale).roundToInt())
      val image = deviceImage(deviceWidth, deviceHeight)
      // The pre-scaled image already matches the device size, so this blit is effectively 1:1
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
      g2.drawImage(image, x, y, logicalWidth, logicalHeight, c)
    }
    finally {
      g2.dispose()
    }
  }

  private fun deviceImage(width: Int, height: Int): BufferedImage {
    cached?.let { if (cachedWidth == width && cachedHeight == height) return it }
    return multiStepScale(source, width, height).also {
      cached = it
      cachedWidth = width
      cachedHeight = height
    }
  }
}

/** High-quality downscale by progressive halving (much sharper than a single big bicubic step). */
private fun multiStepScale(source: BufferedImage, targetWidth: Int, targetHeight: Int): BufferedImage {
  var width = source.width
  var height = source.height
  var current = source
  while (width / 2 >= targetWidth && height / 2 >= targetHeight) {
    width /= 2
    height /= 2
    current = scaleStep(current, width, height)
  }
  return scaleStep(current, targetWidth, targetHeight)
}

private fun scaleStep(source: BufferedImage, width: Int, height: Int): BufferedImage {
  val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
  val g = result.createGraphics()
  g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
  g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
  g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
  g.drawImage(source, 0, 0, width, height, null)
  g.dispose()
  return result
}
