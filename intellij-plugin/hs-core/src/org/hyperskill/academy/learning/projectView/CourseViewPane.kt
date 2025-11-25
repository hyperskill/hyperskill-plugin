/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hyperskill.academy.learning.projectView

import com.intellij.ide.SelectInTarget
import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.AbstractProjectViewPaneWithAsyncSupport
import com.intellij.ide.projectView.impl.ProjectAbstractTreeStructureBase
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.ArrayUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.EducationalCoreIcons.CourseView.CourseTree
import org.hyperskill.academy.learning.CourseSetListener
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.projectView.ProgressUtil.createProgressBar
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer

class CourseViewPane(project: Project) : AbstractProjectViewPaneWithAsyncSupport(project) {

  private lateinit var progressBar: JProgressBar

  private val courseViewComponent: JComponent by lazy { createCourseViewComponent() }

  override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree {
    return object : ProjectViewTree(treeModel) {
      override fun toString(): String = "$title ${super.toString()}"

      override fun createCellRenderer(): TreeCellRenderer {
        val projectViewRenderer = super.createCellRenderer()
        return projectViewRenderer
      }
    }
  }

  override fun createComponent(): JComponent = courseViewComponent

  override fun createComparator(): Comparator<NodeDescriptor<*>> = EduNodeComparator

  private fun createCourseViewComponent(): JComponent {
    super.createComponent()
    CourseViewPaneCustomization.customize(tree)

    val panel = JPanel(BorderLayout())
    panel.background = UIUtil.getTreeBackground()

    panel.add(createProgressPanel(), BorderLayout.NORTH)
    panel.add(tree, BorderLayout.CENTER)

    if (StudyTaskManager.getInstance(myProject).course != null) {
      updateCourseProgress()
    }
    else {
      val connection = myProject.messageBus.connect()
      connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
        override fun courseSet(course: Course) {
          connection.disconnect()
          updateCourseProgress()
        }
      })
    }
    return ScrollPaneFactory.createScrollPane(panel)
  }

  private fun createProgressPanel(): JPanel {
    val panel = JPanel(BorderLayout())

    progressBar = createProgressBar()
    panel.background = UIUtil.getTreeBackground()
    panel.add(progressBar, BorderLayout.NORTH)
    panel.border = JBUI.Borders.emptyBottom(5)
    return panel
  }

  override fun addToolbarActions(actionGroup: DefaultActionGroup) {
    actionGroup.removeAll()
    val group = ActionManager.getInstance().getAction("HyperskillEducational.CourseView.SecondaryActions") as DefaultActionGroup
    for (action in group.childActionsOrStubs) {
      actionGroup.addAction(action).setAsSecondary(true)
    }
  }

  private fun createHeaderRightToolbar(): ActionToolbar {
    val group = ActionManager.getInstance().getAction("HyperskillEducational.CourseView.Header.Right") as ActionGroup
    return ActionManager.getInstance().createActionToolbar(ActionPlaces.PROJECT_VIEW_TOOLBAR, group, true)
  }

  private fun updateCourseProgress() {
    val course = StudyTaskManager.getInstance(myProject).course
    if (course == null) {
      Logger.getInstance(CourseViewPane::class.java).error("course is null")
      return
    }
    updateCourseProgress(ProgressUtil.countProgress(course))
  }

  fun updateCourseProgress(progress: ProgressUtil.CourseProgress) {
    progressBar.maximum = progress.tasksTotalNum
    progressBar.value = progress.tasksSolved
  }

  @TestOnly
  fun getProgressBar(): JProgressBar = progressBar

  override fun createStructure(): ProjectAbstractTreeStructureBase = object : ProjectTreeStructure(myProject, ID), ProjectViewSettings {
    override fun createRoot(project: Project, settings: ViewSettings): AbstractTreeNode<*> {
      return RootNode(myProject, settings)
    }

    override fun getChildElements(element: Any): Array<Any> {
      if (element !is AbstractTreeNode<*>) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY
      }
      val elements = element.children
      elements.forEach { node -> node.parent = element }
      return ArrayUtil.toObjectArray(elements)
    }

    override fun isShowExcludedFiles() = false
  }

  override fun getTitle(): String = EduCoreBundle.message("project.view.course.pane.title")

  override fun getIcon(): Icon = CourseTree
  override fun getId(): String = ID
  override fun getWeight(): Int = 5

  override fun createSelectInTarget(): SelectInTarget {
    return object : ProjectViewSelectInTarget(myProject) {
      override fun getMinorViewId(): String = ID
      override fun toString(): String = ID
    }
  }

  @Suppress("UnstableApiUsage")
  override fun supportsFoldersAlwaysOnTop(): Boolean = false

  @Suppress("UnstableApiUsage")
  override fun supportsSortByType(): Boolean = false

  @Deprecated("Migrate to [uiDataSnapshot] ASAP.")
  override fun getData(dataId: String): Any? {
    if (myProject.isDisposed) return null
    return super.getData(dataId)
  }

  override fun isDefaultPane(project: Project): Boolean = project.isEduProject()

  companion object {
    @NonNls
    const val ID = "HyperskillCourse"
    const val HIDE_SOLVED_LESSONS = "Hyperskill.HideSolvedLessons"

    val STUDY_ITEM: DataKey<StudyItem> = DataKey.create("Hyperskill.studyItem")
  }

  class HideSolvedLessonsAction : DumbAwareToggleAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun isSelected(e: AnActionEvent): Boolean {
      return PropertiesComponent.getInstance().getBoolean(HIDE_SOLVED_LESSONS, false)
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
      PropertiesComponent.getInstance().setValue(HIDE_SOLVED_LESSONS, state)
      val project = e.project ?: return
      ProjectView.getInstance(project).refresh()
    }
  }
}
