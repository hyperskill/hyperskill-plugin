package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.JavaUILibrary
import org.hyperskill.academy.learning.computeUnderProgress
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTopic
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TaskToolWindowTab

/**
 * Constructor is called exclusively in [org.hyperskill.academy.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
class TopicsTab(project: Project) : TaskToolWindowTab(project) {
  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.SWING

  override fun update(task: Task) {
    val course = task.course as? HyperskillCourse
                 ?: error("Topics tab is designed for Hyperskill course, but task is located in different course")
    val topics = course.taskToTopics[task.index - 1]

    val innerPanel = panel {
      row {
        label(EduCoreBundle.message("hyperskill.topics.for.stage")).bold()
      }
        .topGap(TopGap.SMALL)
        .bottomGap(BottomGap.SMALL)
      if (topics != null) {
        for ((index, topic) in topics.withIndex()) {
          row("${index + 1}.") {
            browserLink(topic.title, "https://hyperskill.org/learn/step/${topic.theoryId}/")
              .resizableColumn()
            actionButton(OpenTopic(project, topic))
          }
            .layout(RowLayout.PARENT_GRID)
        }
      }
      else {
        row {
          text(EduCoreBundle.message("hyperskill.topics.not.found"))
        }
      }
    }.apply {
      isOpaque = false
      border = JBUI.Borders.empty(12)
    }
    removeAll()
    add(JBScrollPane(innerPanel).apply { border = JBUI.Borders.empty() })
    innerPanel.background = TaskToolWindowView.getTaskDescriptionBackgroundColor()
  }

  private class OpenTopic(
    private val project: Project,
    private val topic: HyperskillTopic
  ) : AnAction(
    EduCoreBundle.message("hyperskill.topics.solve"),
    EduCoreBundle.message("hyperskill.topics.solve"),
    AllIcons.Actions.Download
  ) {
    override fun actionPerformed(e: AnActionEvent) {
      computeUnderProgress(project, EduCoreBundle.message("hyperskill.topics.fetch"), true) {
        openTopic(project, topic)
      }
    }
  }
}
