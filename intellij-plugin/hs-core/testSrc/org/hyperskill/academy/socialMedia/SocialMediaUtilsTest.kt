package org.hyperskill.academy.socialMedia

import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillCourseWithFiles
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class SocialMediaUtilsTest : EduTestCase() {

  @Test
  fun `test X share intent passes the link only inside the text`() {
    val task = createProjectTask()

    val params = queryParams(SocialMediaUtils.buildXShareUrl(task))

    // X appends the `url` parameter to the text, so passing it would put a second link into the post
    assertEquals(setOf("text"), params.keys)
    assertSingleLink(params.getValue("text"))
    assertEquals(SocialMediaUtils.getShareText(task), params.getValue("text"))
  }

  @Test
  fun `test LinkedIn share intent passes the link only inside the text`() {
    val task = createProjectTask()

    val params = queryParams(SocialMediaUtils.buildLinkedInShareUrl(task))

    assertEquals(setOf("shareActive", "text"), params.keys)
    assertSingleLink(params.getValue("text"))
    assertEquals(SocialMediaUtils.getShareText(task), params.getValue("text"))
  }

  @Test
  fun `test dialog message shows the link without tracking parameters`() {
    val task = createProjectTask()

    val message = SocialMediaUtils.getDisplayMessage(task)

    assertEquals(1, message.occurrencesOf(COURSES_LINK))
    assertFalse("Tracking parameters must not be shown in the dialog", message.contains("utm_"))
  }

  private fun assertSingleLink(text: String) {
    // A bare `hyperskill.org/courses` is turned into a link by X too, so it must not appear next to the tracked link
    assertEquals("The post must contain exactly one link: $text", 1, text.occurrencesOf(COURSES_LINK))
    assertTrue("The post must end with the tracked link: $text", text.endsWith(SocialMediaUtils.SHARE_URL))
  }

  private fun queryParams(url: String): Map<String, String> =
    url.substringAfter('?').split('&').associate {
      it.substringBefore('=') to URLDecoder.decode(it.substringAfter('='), StandardCharsets.UTF_8)
    }

  private fun String.occurrencesOf(substring: String): Int = split(substring).size - 1

  private fun createProjectTask(): Task {
    val course = hyperskillCourseWithFiles {
      frameworkLesson("Project") {
        eduTask("Task1") {
          taskFile("task.txt")
        }
      }
    }
    return course.getProjectLesson()?.getTask("Task1") ?: error("No Task1")
  }

  companion object {
    private const val COURSES_LINK = "hyperskill.org/courses"
  }
}
