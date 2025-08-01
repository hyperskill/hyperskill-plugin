package org.hyperskill.academy.coursecreator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import org.hyperskill.academy.coursecreator.actions.BinaryContentsFromDisk
import org.hyperskill.academy.coursecreator.actions.TextualContentsFromDisk
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.getTask
import org.hyperskill.academy.learning.isToEncodeContent
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.TASK_CONFIG
import java.io.IOException

object AdditionalFilesUtils {
  private val LOG = Logger.getInstance(AdditionalFilesUtils::class.java)

  /**
   * @param saveDocuments specifies whether to save all documents in open editors before collecting additional files.
   * Default is `true`.
   * If [saveDocuments] is `false`, no write action will be performed during the call.
   *
   * @param detectTaskFoldersByContents controls the way to determine whether a folder is a task folder.
   * Additional files are not searched inside task folders. Default is `false`.
   * When [detectTaskFoldersByContents] is `true`, the folder is considered to be a task folder, if it contains
   * the `task-info.yaml` file.
   * When [detectTaskFoldersByContents] is `false`, the `project.course` object is inspected to find the task with that folder.
   * Use `detectTaskFoldersByContents=true` in case the `project.course` object is unavailable.
   */
  fun collectAdditionalFiles(
    courseConfigurator: EduConfigurator<*>?,
    project: Project,
    saveDocuments: Boolean = true,
    detectTaskFoldersByContents: Boolean = false
  ): List<EduFile> {
    if (courseConfigurator == null) return listOf()

    if (saveDocuments) {
      ApplicationManager.getApplication().invokeAndWait { FileDocumentManager.getInstance().saveAllDocuments() }
    }

    val fileVisitor = additionalFilesVisitor(project, courseConfigurator, detectTaskFoldersByContents)
    VfsUtilCore.visitChildrenRecursively(project.courseDir, fileVisitor)
    return fileVisitor.additionalTaskFiles
  }

  private fun additionalFilesVisitor(project: Project, courseConfigurator: EduConfigurator<*>, detectTaskFoldersByContents: Boolean) =
    object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
      // we take the course ignore rules once, and we are sure they are not changed while course archive is being created
      val additionalTaskFiles = mutableListOf<EduFile>()

      override fun visitFile(file: VirtualFile): Boolean {
        if (file.isDirectory) {
          // All files inside task directory are already handled by `CCVirtualFileListener`
          // so here we don't need to process them again
          val isTaskFolder = if (detectTaskFoldersByContents) {
            file.findChild(TASK_CONFIG) != null
          }
          else {
            file.getTask(project) != null
          }

          return !isTaskFolder
        }

        addToAdditionalFiles(file, project)
        return false
      }

      private fun addToAdditionalFiles(file: VirtualFile, project: Project) {
        try {
          createAdditionalTaskFile(file, project)?.also { taskFile -> additionalTaskFiles.add(taskFile) }
        }
        catch (e: IOException) {
          LOG.error(e)
        }
      }

      private fun createAdditionalTaskFile(file: VirtualFile, project: Project): EduFile? {
        val path = VfsUtilCore.getRelativePath(file, project.courseDir) ?: return null
        val contents = if (file.isToEncodeContent) {
          BinaryContentsFromDisk(file)
        }
        else {
          TextualContentsFromDisk(file)
        }
        return EduFile(path, contents)
      }
    }
}
