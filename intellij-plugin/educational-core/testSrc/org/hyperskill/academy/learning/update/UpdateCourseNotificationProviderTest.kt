package org.hyperskill.academy.learning.update

import org.hyperskill.academy.learning.NotificationsTestBase
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.navigation.NavigationUtils.getFirstTask
import org.hyperskill.academy.learning.stepik.UpdateCourseNotificationProvider
import org.junit.Test

class UpdateCourseNotificationProviderTest : NotificationsTestBase() {

  @Test
  fun `test update course notification not shown`() {
    val course = createCourse(isUpToDate = true)
    val virtualFile = getFirstTask(course)!!.getTaskFile("Task.txt")!!.getVirtualFile(project)!!
    checkNoEditorNotification<UpdateCourseNotificationProvider>(virtualFile)
  }

  private fun createCourse(isUpToDate: Boolean): HyperskillCourse {
    val course = courseWithFiles {
      lesson {
        eduTask("task1") {
          taskFile("Task.txt")
        }
      }
    } as HyperskillCourse
    return course
  }
}
