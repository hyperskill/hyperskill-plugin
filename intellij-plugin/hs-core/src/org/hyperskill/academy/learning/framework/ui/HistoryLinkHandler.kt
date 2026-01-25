package org.hyperskill.academy.learning.framework.ui

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.actions.ApplyCodeAction
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.ext.isTestFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.framework.impl.FrameworkStorage
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.links.SwingToolWindowLinkHandler

/**
 * Link handler for History tab that shows diff between current files and a commit snapshot.
 */
class HistoryLinkHandler(
  project: Project,
  private val task: Task,
  private val storage: FrameworkStorage
) : SwingToolWindowLinkHandler(project) {

  override fun process(url: String, referUrl: String?): Boolean {
    if (!url.startsWith(COMMIT_DIFF_URL)) {
      return false
    }

    val commitHash = url.substringAfter(COMMIT_DIFF_URL)
    showDiff(commitHash)
    return true
  }

  private fun showDiff(commitHash: String) {
    val commit = storage.getCommit(commitHash) ?: return
    val snapshot = storage.getSnapshotByHash(commit.snapshotHash) ?: return

    val taskFiles = task.taskFiles.values.filter { it.isVisible && !it.isTestFile }
    val requests = taskFiles.mapNotNull { taskFile ->
      val virtualFile = taskFile.getVirtualFile(project) ?: return@mapNotNull null
      val currentText = FileDocumentManager.getInstance().getDocument(virtualFile)?.text ?: return@mapNotNull null

      val snapshotText = snapshot[taskFile.name] ?: return@mapNotNull null

      val currentContent = DiffContentFactory.getInstance().create(currentText, virtualFile.fileType)
      val snapshotContent = DiffContentFactory.getInstance().create(snapshotText, virtualFile.fileType)

      SimpleDiffRequest(
        EduCoreBundle.message("history.compare"),
        currentContent,
        snapshotContent,
        EduCoreBundle.message("submissions.local"),
        EduCoreBundle.message("history.snapshot")
      )
    }

    if (requests.isEmpty()) return

    val filePaths = taskFiles.mapNotNull { it.getVirtualFile(project)?.path }

    runInEdt {
      val diffRequestChain = SimpleDiffRequestChain(requests)
      diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, filePaths)
      DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
    }
  }

  companion object {
    private const val HISTORY_PROTOCOL = "history://"
    private const val COMMIT_DIFF_URL = "${HISTORY_PROTOCOL}diff/"

    fun getCommitDiffLink(commitHash: String): String = "$COMMIT_DIFF_URL$commitHash"
  }
}
