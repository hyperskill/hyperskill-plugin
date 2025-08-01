package org.hyperskill.academy.coursecreator.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.hyperskill.academy.coursecreator.framework.SyncChangesStateManager
import org.hyperskill.academy.learning.getTaskFile

open class CCFileNode(
  project: Project,
  value: PsiFile,
  viewSettings: ViewSettings
) : PsiFileNode(project, value, viewSettings) {
  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)
    if (data is CCFilePresentationData) {
      data.syncChangesState = null
      val taskFile = value.virtualFile.getTaskFile(project) ?: return
      data.syncChangesState = SyncChangesStateManager.getInstance(project).getSyncChangesState(taskFile)
    }
  }

  override fun createPresentation(): PresentationData {
    val data = CCFilePresentationData()
    val taskFile = value.virtualFile.getTaskFile(project) ?: return data
    data.syncChangesState = SyncChangesStateManager.getInstance(project).getSyncChangesState(taskFile)
    return data
  }
}
