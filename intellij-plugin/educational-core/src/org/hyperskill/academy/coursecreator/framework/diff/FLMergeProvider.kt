package org.hyperskill.academy.coursecreator.framework.diff

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vcs.merge.MergeData
import com.intellij.openapi.vcs.merge.MergeProvider2
import com.intellij.openapi.vcs.merge.MergeSession
import com.intellij.openapi.vcs.merge.MergeSession.Resolution
import com.intellij.openapi.vcs.merge.MergeSessionEx
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.ColumnInfo
import org.hyperskill.academy.learning.framework.impl.FLTaskState
import org.hyperskill.academy.learning.messages.EduCoreBundle

class FLMergeProvider(
  private val leftState: FLTaskState,
  private val baseState: FLTaskState,
  private val rightState: FLTaskState,
  private val initialBaseState: FLTaskState = baseState,
) : MergeProvider2 {
  override fun loadRevisions(file: VirtualFile): MergeData {
    return MergeData().apply {
      val filePath = file.path
      ORIGINAL = baseState[filePath]?.encodeToByteArray() ?: emptyContent
      CURRENT = leftState[filePath]?.encodeToByteArray() ?: emptyContent
      LAST = rightState[filePath]?.encodeToByteArray() ?: emptyContent
    }
  }

  override fun conflictResolvedForFile(file: VirtualFile) {}

  override fun isBinary(file: VirtualFile): Boolean {
    return file.fileType.isBinary
  }

  override fun createMergeSession(files: List<VirtualFile>): MergeSession {
    return FLMergeSession()
  }

  inner class FLMergeSession : MergeSessionEx {
    override fun getMergeInfoColumns(): Array<ColumnInfo<out Any, out Any>> {
      return arrayOf(
        StatusColumn("Current task", true),
        StatusColumn("Target task", false)
      )
    }

    override fun canMerge(file: VirtualFile): Boolean {
      return true
    }

    override fun conflictResolvedForFile(file: VirtualFile, resolution: Resolution) {}

    override fun conflictResolvedForFiles(files: List<VirtualFile>, resolution: Resolution) {}

    override fun acceptFilesRevisions(files: List<VirtualFile>, resolution: Resolution) {
      for (file in files) {
        require(file is FLLightVirtualFile)
      }
      for (file in files) {
        val filePath = file.path
        val value = if (resolution == Resolution.AcceptedYours) {
          leftState[filePath]
        }
        else {
          rightState[filePath]
        }

        if (value == null) {
          // file is deleted
          // invalidate file, so it can be checked by file.exists()
          file.delete(javaClass)
        }
        else {
          val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) } ?: return

          @Suppress("UnstableApiUsage")
          invokeAndWaitIfNeeded {
            runWriteAction {
              document.setText(value)
            }
          }
        }
      }
    }
  }

  private inner class StatusColumn(
    defaultName: String,
    private val isLeft: Boolean,
  ) : ColumnInfo<VirtualFile, String>(defaultName) {
    private val defaultGap = 10

    override fun valueOf(item: VirtualFile?): String {
      if (item == null) return ""
      val filePath = item.path
      val baseContent = initialBaseState[filePath]
      val changedContent = if (isLeft) {
        leftState[filePath]
      }
      else {
        rightState[filePath]
      }
      if (baseContent == null) {
        if (changedContent == null) {
          return "-"
        }
        return EduCoreBundle.message("action.HyperskillEducational.Educator.SyncChangesWithNextTasks.MergeDialog.added")
      }
      if (changedContent == null) {
        return EduCoreBundle.message("action.HyperskillEducational.Educator.SyncChangesWithNextTasks.MergeDialog.deleted")
      }
      return EduCoreBundle.message("action.HyperskillEducational.Educator.SyncChangesWithNextTasks.MergeDialog.modified")
    }

    override fun getAdditionalWidth(): Int {
      return defaultGap
    }
  }

  companion object {
    private val emptyContent = ByteArray(0)
  }
}