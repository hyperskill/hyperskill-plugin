package org.hyperskill.academy.learning.github

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

interface PostToGithubActionProvider {

  fun postToGitHub(project: Project, file: VirtualFile)

  companion object {
    val EP_NAME: ExtensionPointName<PostToGithubActionProvider> = ExtensionPointName.create("HyperskillEducational.postToGithub")

    fun first(): PostToGithubActionProvider? = EP_NAME.extensionList.firstOrNull()
  }
}