package org.hyperskill.academy.learning.stepik.hyperskill.api

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.SolutionLoaderBase
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.CheckStatus.Companion.toCheckStatus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.CORRECT
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.CodeTask
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.UnsupportedTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.api.CodeTaskReply
import org.hyperskill.academy.learning.stepik.api.StepikBasedSubmission
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator
import org.hyperskill.academy.learning.stepik.hyperskill.markStageAsCompleted
import org.hyperskill.academy.learning.stepik.hyperskill.openSelectedStage
import org.hyperskill.academy.learning.submissions.Submission

@Service(Service.Level.PROJECT)
class HyperskillSolutionLoader(project: Project) : SolutionLoaderBase(project) {

  override fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions {
    // submission.taskId can differ from task.id because some hyperskill submissions were stored on stepik and got stepik step ID instead
    // of hyperskill task ID, see EDU-5186
    val lastSubmission: Submission = submissions.firstOrNull { it.taskId == task.id }
                                     ?: return TaskSolutions.EMPTY
    if (lastSubmission !is StepikBasedSubmission)
      error(
        "Hyperskill submission ${lastSubmission.id} for task ${task.name} is not instance of ${StepikBasedSubmission::class.simpleName} class"
      )

    val allFiles: Map<String, Solution> = when (task) {
      is EduTask -> lastSubmission.eduTaskFiles
      is CodeTask -> lastSubmission.codeTaskFiles(task)
      is UnsupportedTask -> emptyMap()
      else -> {
        LOG.warn("Solutions for task ${task.name} of type ${task::class.simpleName} not loaded")
        emptyMap()
      }
    }

    LOG.info(
      "loadSolution: task='${task.name}' (id=${task.id}), submissionId=${lastSubmission.id}, status=${lastSubmission.status}, " +
             "allFiles=${allFiles.size} [${allFiles.entries.joinToString { "${it.key}:${it.value.text.length}chars,visible=${it.value.isVisible}" }}]"
    )

    val files = allFiles.filter { (_, solution) -> solution.isVisible }
    if (files.size != allFiles.size) {
      LOG.warn("loadSolution: task='${task.name}' - filtered out ${allFiles.size - files.size} invisible files!")
    }

    return TaskSolutions(lastSubmission.time, lastSubmission.status?.toCheckStatus() ?: CheckStatus.Unchecked, files, submissionId = lastSubmission.id?.toLong())
  }

  private val StepikBasedSubmission.eduTaskFiles: Map<String, Solution>
    get() = solutionFiles?.associate { it.name to Solution(it.text, it.isVisible) } ?: emptyMap()

  private fun StepikBasedSubmission.codeTaskFiles(task: CodeTask): Map<String, Solution> {
    val codeFromServer = (reply as? CodeTaskReply)?.code ?: return emptyMap()
    val configurator = task.course.configurator as? HyperskillConfigurator ?: return emptyMap()
    val taskFile = configurator.getCodeTaskFile(project, task) ?: return emptyMap()
    return mapOf(taskFile.name to Solution(codeFromServer, true))
  }

  override fun updateTasks(
    course: Course,
    tasks: List<Task>,
    submissions: List<Submission>,
    progressIndicator: ProgressIndicator?,
    force: Boolean
  ) {
    super.updateTasks(course, tasks, submissions, progressIndicator, force)
    runInEdt {
      progressIndicator?.text = EduCoreBundle.message("update.setting.stage")
      openSelectedStage(course, project)
    }
  }

  override fun updateTask(project: Project, task: Task, submissions: List<Submission>, force: Boolean): Boolean {
    val course = task.course as HyperskillCourse
    if (task.lesson == course.getProjectLesson() && submissions.any { it.taskId == task.id && it.status == CORRECT }) {
      markStageAsCompleted(task)
    }
    return super.updateTask(project, task, submissions, force)
  }

  companion object {
    fun getInstance(project: Project): HyperskillSolutionLoader = project.service()

    private val LOG = Logger.getInstance(HyperskillSolutionLoader::class.java)
  }
}

