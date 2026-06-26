package org.hyperskill.academy.socialMedia

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.CheckListener
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.socialMedia.suggestToPostDialog.createSuggestToPostDialogUI

/**
 * Suggests sharing the achievement once a Hyperskill project is fully solved.
 * See [SocialMediaUtils.shouldSuggestToPost] for the exact policy.
 */
class SuggestToPostOnProjectCompletionListener : CheckListener {

  private var statusBeforeCheck: CheckStatus? = null

  override fun beforeCheck(project: Project, task: Task) {
    statusBeforeCheck = task.status
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val statusBefore = statusBeforeCheck ?: return
    statusBeforeCheck = null

    if (!SocialMediaSettings.getInstance().askToPost) return
    if (!SocialMediaUtils.shouldSuggestToPost(project, task, statusBefore)) return

    createSuggestToPostDialogUI(
      project,
      SocialMediaUtils.getDisplayMessage(task),
      SocialMediaUtils.buildXShareUrl(task),
      SocialMediaUtils.buildLinkedInShareUrl(task)
    ).showAndGet()
  }
}
