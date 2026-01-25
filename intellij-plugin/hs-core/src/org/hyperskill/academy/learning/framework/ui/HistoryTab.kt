package org.hyperskill.academy.learning.framework.ui

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.JavaUILibrary
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.framework.impl.FrameworkStorage
import org.hyperskill.academy.learning.framework.impl.getStorageRef
import org.hyperskill.academy.learning.framework.storage.FileBasedFrameworkStorage
import org.hyperskill.academy.learning.invokeLater
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.StyleManager
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.SwingTextPanel
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TaskToolWindowCardTextTab
import java.io.IOException
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tab showing local changes history from Framework Storage.
 * Similar to SubmissionsTab but shows local commits instead of server submissions.
 */
class HistoryTab(project: Project) : TaskToolWindowCardTextTab(project) {

  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.SWING

  private val panel: SwingTextPanel
    get() = cards().first() as SwingTextPanel

  override fun update(task: Task) {
    val lesson = task.lesson
    if (lesson !is FrameworkLesson) {
      showEmpty()
      return
    }

    val storagePath = Paths.get(project.basePath!!, ".idea", "frameworkLessonHistory", "storage")
    val storage = try {
      FrameworkStorage(storagePath)
    } catch (e: Exception) {
      showEmpty()
      return
    }

    try {
      val ref = task.getStorageRef()
      if (!storage.hasRef(ref)) {
        showEmpty()
        storage.dispose()
        return
      }

      val commits = collectCommitsForRef(storage, ref)
      if (commits.isEmpty()) {
        showEmpty()
        storage.dispose()
        return
      }

      val html = buildCommitsHtml(commits, task)
      project.invokeLater {
        panel.updateLinkHandler(HistoryLinkHandler(project, task, storage))
        panel.setText(html)
      }
    } catch (e: IOException) {
      showEmpty()
      storage.dispose()
    }
  }

  private fun showEmpty() {
    project.invokeLater {
      panel.updateLinkHandler(null)
      panel.setText("<p style=\"${StyleManager().textStyleHeader}\">${EduCoreBundle.message("history.empty")}</p>")
    }
  }

  private fun collectCommitsForRef(storage: FrameworkStorage, ref: String): List<CommitInfo> {
    val commits = mutableListOf<CommitInfo>()
    val visited = mutableSetOf<String>()

    fun traverse(commitHash: String, depth: Int) {
      if (commitHash in visited || depth > 20) return
      visited.add(commitHash)

      val commit = storage.getCommit(commitHash) ?: return
      commits.add(CommitInfo(
        hash = commitHash,
        message = commit.message,
        timestamp = commit.timestamp,
        isMerge = commit.parentHashes.size > 1
      ))

      for (parentHash in commit.parentHashes) {
        traverse(parentHash, depth + 1)
      }
    }

    val refCommitHash = storage.resolveRef(ref) ?: return emptyList()
    traverse(refCommitHash, 0)

    return commits.sortedByDescending { it.timestamp }
  }

  private fun buildCommitsHtml(commits: List<CommitInfo>, task: Task): String {
    val styleManager = StyleManager()
    val dateFormat = SimpleDateFormat("dd MMM HH:mm")
    val sb = StringBuilder()

    sb.append("<ul style=\"list-style-type:none;margin:0;padding:0;\">")

    for (commit in commits) {
      val date = dateFormat.format(Date(commit.timestamp))
      val shortHash = commit.hash.take(7)
      val mergeIndicator = if (commit.isMerge) " <span style=\"color:#D97706;font-weight:bold;\">[merge]</span>" else ""
      val message = commit.message.take(50).let { if (commit.message.length > 50) "$it..." else it }

      sb.append("<li style=\"margin-bottom:8px;\">")
      sb.append("<a style=\"${styleManager.textStyleHeader}\" href=\"${HistoryLinkHandler.getCommitDiffLink(commit.hash)}\">")
      sb.append("<code style=\"color:#CF222E;\">$shortHash</code>")
      sb.append(" $date$mergeIndicator")
      sb.append("</a>")
      sb.append("<br/><span style=\"color:#6E7781;font-size:0.9em;\">$message</span>")
      sb.append("</li>")
    }

    sb.append("</ul>")
    return sb.toString()
  }

  private data class CommitInfo(
    val hash: String,
    val message: String,
    val timestamp: Long,
    val isMerge: Boolean
  )
}
