package org.hyperskill.academy.github

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.github.PostToGithubActionProvider

class PostToGithubProviderImpl : PostToGithubActionProvider {

  override fun postToGitHub(project: Project, file: VirtualFile) {
    val action = ActionManager.getInstance().getAction("Github.ShareProject") ?: return
    val dataContext = DataContext { dataId ->
      when {
        CommonDataKeys.PROJECT.`is`(dataId) -> project
        CommonDataKeys.VIRTUAL_FILE.`is`(dataId) -> file
        else -> null
      }
    }
    val event = AnActionEvent.createFromAnAction(action, null, "", dataContext)
    action.actionPerformed(event)
  }
}
