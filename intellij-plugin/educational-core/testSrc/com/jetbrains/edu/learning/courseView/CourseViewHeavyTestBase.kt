package org.hyperskill.academy.learning.courseView

import com.intellij.ide.projectView.ProjectView
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.ProjectViewTestUtil
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings
import org.hyperskill.academy.learning.projectView.CourseViewPane

abstract class CourseViewHeavyTestBase : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  protected fun createCourseAndChangeView(course: Course, openFirstTask: Boolean = true): ProjectView {
    createCourseStructure(course)

    // can't do it in setUp because project is not opened at that point
    ProjectViewTestUtil.setupImpl(project, true)

    return ProjectView.getInstance(project).apply {
      refresh()
      changeView(CourseViewPane.ID)
      if (openFirstTask) {
        NavigationUtils.openFirstTask(course, project)
      }
      PlatformTestUtil.waitWhileBusy(currentProjectViewPane.tree)
    }
  }
}
