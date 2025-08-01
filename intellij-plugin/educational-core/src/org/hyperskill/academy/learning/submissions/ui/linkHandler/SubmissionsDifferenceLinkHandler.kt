package org.hyperskill.academy.learning.submissions.ui.linkHandler

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.actions.ApplyCodeAction
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.ext.isTestFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.submissions.*
import org.hyperskill.academy.learning.submissions.ui.SubmissionsTab.Companion.SUBMISSION_PROTOCOL
import org.hyperskill.academy.learning.taskToolWindow.links.SwingToolWindowLinkHandler

class SubmissionsDifferenceLinkHandler(
  project: Project,
  private val task: Task,
  private val submissionsManager: SubmissionsManager,
  private val isCommunity: Boolean = false
) : SwingToolWindowLinkHandler(project) {

  override fun process(url: String, referUrl: String?): Boolean = with(url) {
    when {
      startsWith(SUBMISSION_DIFF_URL) -> {
        val submissionId = url.substringAfter(SUBMISSION_DIFF_URL).toInt()
        ApplicationManager.getApplication().executeOnPooledThread {
          val submission = submissionsManager.getSubmissionWithSolutionText(task, submissionId) ?: return@executeOnPooledThread
          runInEdt {
            showDiff(project, task, submission, isCommunity)
          }
        }
        return true
      }

      startsWith(SHOW_MORE_SOLUTIONS) -> {
        return true
      }
    }

    return false
  }

  private fun showDiff(project: Project, task: Task, submission: Submission, isCommunity: Boolean) {
    val taskFiles = task.taskFiles.values.toMutableList()
    val submissionTexts = submission.getSubmissionTexts(task.name) ?: return
    val submissionTaskFiles = taskFiles.filter { it.isVisible && !it.isTestFile }
    val submissionTaskFilePaths = mutableListOf<String>()
    val requests = submissionTaskFiles.mapNotNull {
      val virtualFile = it.getVirtualFile(project) ?: error("VirtualFile for ${it.name} not found")
      val documentText = FileDocumentManager.getInstance().getDocument(virtualFile)?.text
      val currentFileContent = if (documentText != null) DiffContentFactory.getInstance().create(documentText, virtualFile.fileType)
      else null
      val submissionText = submissionTexts[it.name] ?: submissionTexts[task.name]
      if (submissionText == null || currentFileContent == null) {
        null
      }
      else {
        submissionTaskFilePaths.add(virtualFile.path)
        val submissionFileContent = DiffContentFactory.getInstance().create(submissionText.removeAllTags(), virtualFile.fileType)
        createSimpleDiffRequest(currentFileContent, submissionFileContent, submission, isCommunity)
      }
    }
    val diffRequestChain = SimpleDiffRequestChain(requests)
    diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, submissionTaskFilePaths)
    DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
  }

  private fun String.removeAllTags(): String = replace(OPEN_PLACEHOLDER_TAG.toRegex(), "").replace(CLOSE_PLACEHOLDER_TAG.toRegex(), "")

  private fun createSimpleDiffRequest(
    currentContent: DocumentContent, submissionContent: DocumentContent, submission: Submission, isCommunity: Boolean
  ): SimpleDiffRequest {
    val (title, title2) = if (!isCommunity) {
      EduCoreBundle.message("submissions.compare") to EduCoreBundle.message("submissions.submission")
    }
    else {
      val time = submission.time
      val formattedDate = time?.let { formatDate(time) } ?: ""
      EduCoreBundle.message("submissions.compare.community", formattedDate) to EduCoreBundle.message("submissions.community")
    }

    return SimpleDiffRequest(title, currentContent, submissionContent, EduCoreBundle.message("submissions.local"), title2)
  }

  companion object {
    private const val SUBMISSION_DIFF_URL = "${SUBMISSION_PROTOCOL}diff/"
    private const val SHOW_MORE_SOLUTIONS = "${SUBMISSION_PROTOCOL}more/"

    fun getSubmissionDiffLink(submissionId: Int?): String = "$SUBMISSION_DIFF_URL$submissionId"

  }
}