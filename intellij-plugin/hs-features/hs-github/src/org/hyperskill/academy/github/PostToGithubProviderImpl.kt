package org.hyperskill.academy.github

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.github.PostToGithubActionProvider

class PostToGithubProviderImpl : PostToGithubActionProvider {

  override fun postToGitHub(project: Project, file: VirtualFile) {
    val action = ActionManager.getInstance().getAction("Github.ShareProject") ?: return
    val dataContext = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, project)
      .add(CommonDataKeys.VIRTUAL_FILE, file)
      .build()
    val event = AnActionEvent.createEvent(action, dataContext, null, "", ActionUiKind.NONE, null)
    ActionUtil.performAction(action, event)
  }
}
