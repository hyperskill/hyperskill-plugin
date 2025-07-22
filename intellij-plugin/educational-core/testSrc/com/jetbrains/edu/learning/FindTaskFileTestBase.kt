package org.hyperskill.academy.learning

import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase
import org.hyperskill.academy.learning.newproject.EduProjectSettings

abstract class FindTaskFileTestBase<Settings : EduProjectSettings> : CourseGenerationTestBase<Settings>() {

  protected fun doTestGetTaskDir(filePath: String, taskDirPath: String) {
    val file = findFile(filePath)
    val expectedTaskDir = findFile(taskDirPath)
    assertEquals(expectedTaskDir, file.getTaskDir(project))
  }

  protected fun doTestGetTaskForFile(course: Course, filePath: String, expectedTask: (Course) -> Task) {
    val file = findFile(filePath)
    val task = expectedTask(course)
    val taskFromUtils = file.getContainingTask(project)
    assertEquals(course, StudyTaskManager.getInstance(project).course)
    assertEquals("tasks: " + task.name + " " + taskFromUtils!!.name, task, taskFromUtils)
  }

  protected fun doTestGetTaskFile(course: Course, filePath: String, expectedTaskFile: (Course) -> TaskFile) {
    val file = findFile(filePath)
    val taskFile = expectedTaskFile(course)
    val taskFileFromUtils = file.getTaskFile(project)
    assertEquals(course, StudyTaskManager.getInstance(project).course)
    assertEquals("task files: " + taskFile.name + " " + taskFileFromUtils!!.name, taskFile, taskFileFromUtils)
  }
}
