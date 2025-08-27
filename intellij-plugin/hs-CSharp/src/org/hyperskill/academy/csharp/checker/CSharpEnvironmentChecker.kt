package org.hyperskill.academy.csharp.checker

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.reactive.valueOrDefault
import com.jetbrains.rider.plugins.unity.UnityProjectDiscoverer
import com.jetbrains.rider.plugins.unity.actions.StartUnityAction
import com.jetbrains.rider.plugins.unity.isUnityProjectFolder
import com.jetbrains.rider.plugins.unity.model.UnityEditorState
import com.jetbrains.rider.plugins.unity.model.frontendBackend.frontendBackendModel
import com.jetbrains.rider.projectDir
import com.jetbrains.rider.projectView.solution
import org.hyperskill.academy.csharp.messages.EduCSharpBundle
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task

class CSharpEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    // Condition borrowed from Unity Support implementation:
    // checks whether the project (opened as a folder or as a solution) might be a Unity project
    if (!project.isUnityProjectFolder.value
        && !UnityProjectDiscoverer.searchUpForFolderWithUnityFileStructure(project.projectDir).first
    ) {
      return null
    }
    val model = project.solution.frontendBackendModel
    val unityEditorState = invokeAndWaitIfNeeded {
      model.unityEditorState.valueOrDefault(UnityEditorState.Disconnected)
    }
    if (unityEditorState == UnityEditorState.Disconnected) {
      return CheckResult(
        CheckStatus.Unchecked,
        EduCSharpBundle.message("error.could.not.launch.unity.test.session"),
        hyperlinkAction = {
          StartUnityAction.startUnity(project)
        })
    }
    return null
  }
}