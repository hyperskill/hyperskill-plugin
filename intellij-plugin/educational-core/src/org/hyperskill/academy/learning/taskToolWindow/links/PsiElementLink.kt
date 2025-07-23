package org.hyperskill.academy.learning.taskToolWindow.links

import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.NavigatablePsiElement
import org.hyperskill.academy.learning.messages.EduCoreBundle

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class PsiElementLink(link: String) : TaskDescriptionLink<NavigatablePsiElement, NavigatablePsiElement?>(link) {

  override fun resolve(project: Project): NavigatablePsiElement? {
    val qualifiedName = linkPath
    val dumbService = DumbService.getInstance(project)
    if (dumbService.isDumb) {
      val message = ActionUtil.getActionUnavailableMessage(EduCoreBundle.message("label.navigation"))
      dumbService.showDumbModeNotification(message)
      return null
    }

    for (provider in QualifiedNameProvider.EP_NAME.extensionList) {
      val element = provider.qualifiedNameToElement(qualifiedName, project)
      if (element is NavigatablePsiElement) {
        if (element.canNavigate()) {
          return element
        }
      }
    }
    return null
  }

  override fun open(project: Project, element: NavigatablePsiElement) {
    runInEdt {
      if (element.isValid) {
        element.navigate(true)
      }
    }
  }

  override suspend fun validate(project: Project, element: NavigatablePsiElement?): String? {
    return if (element == null) "Failed to find an element by `$linkPath` reference" else null
  }
}
