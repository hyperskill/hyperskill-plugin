package com.jetbrains.edu.learning.editor

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.problems.WolfTheProblemSolver
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.getTaskFile
import com.jetbrains.edu.learning.statistics.EduLaunchesReporter.sendStats
import com.jetbrains.edu.learning.stepik.hyperskill.markHyperskillTheoryTaskAsCompleted
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.saveItem

class EduEditorFactoryListener : EditorFactoryListener {

  override fun editorCreated(event: EditorFactoryEvent) {
    val editor = event.editor
    val project = editor.project ?: return
    val course = StudyTaskManager.getInstance(project).course ?: return
    val openedFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return
    val taskFile = openedFile.getTaskFile(project) ?: return
    WolfTheProblemSolver.getInstance(project).clearProblems(openedFile)
    val task = taskFile.task
    markTheoryTaskCompleted(project, task)
    sendStats(course)
  }

  override fun editorReleased(event: EditorFactoryEvent) = event.editor.selectionModel.removeSelection()

  private fun markTheoryTaskCompleted(project: Project, task: Task) {
    if (task !is TheoryTask) return
    val course = task.course
    if (course.isStudy && task.postSubmissionOnOpen && task.status !== CheckStatus.Solved) {
      if (course is HyperskillCourse) {
        markHyperskillTheoryTaskAsCompleted(project, task)
      }
      task.status = CheckStatus.Solved
      saveItem(task)
      ProjectView.getInstance(project).refresh()
    }
  }
}
