package org.hyperskill.academy.coursecreator.framework.diff

import com.intellij.openapi.vcs.history.VcsRevisionNumber
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.platform.MergeDialogCustomizerCompat

class FLMergeDialogCustomizer(
  private val currentTaskName: String,
  private val targetTaskName: String,
) : MergeDialogCustomizerCompat() {
  override fun getColumnNames(): List<String> {
    return listOf(currentTaskName, targetTaskName)
  }

  override fun getMergeWindowTitle(file: VirtualFile): String {
    return EduCoreBundle.message("action.HyperskillEducational.Educator.SyncChangesWithNextTasks.MergeDialog.MergeWindow.title", file.path)
  }

  override fun getMultipleFileDialogTitle(): String {
    return EduCoreBundle.message("action.HyperskillEducational.Educator.SyncChangesWithNextTasks.MergeDialog.MultipleFileDialog.title")
  }

  override fun getLeftPanelTitle(file: VirtualFile): String {
    return EduCoreBundle.message(
      "action.HyperskillEducational.Educator.SyncChangesWithNextTasks.MergeDialog.LeftPanel.title",
      currentTaskName
    )
  }

  override fun getCenterPanelTitle(file: VirtualFile): String {
    return EduCoreBundle.message("action.HyperskillEducational.Educator.SyncChangesWithNextTasks.MergeDialog.CenterPanel.title")
  }

  override fun getRightPanelTitle(file: VirtualFile, revisionNumber: VcsRevisionNumber?): String {
    return EduCoreBundle.message(
      "action.HyperskillEducational.Educator.SyncChangesWithNextTasks.MergeDialog.RightPanel.title",
      targetTaskName
    )
  }
}