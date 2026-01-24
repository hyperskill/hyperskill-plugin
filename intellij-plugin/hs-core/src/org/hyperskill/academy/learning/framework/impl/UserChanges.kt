package org.hyperskill.academy.learning.framework.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.apache.commons.codec.binary.Base64
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.courseGeneration.macro.EduMacroUtils
import org.hyperskill.academy.learning.framework.storage.Change
import org.hyperskill.academy.learning.framework.storage.UserChanges
import java.io.IOException

private val LOG: Logger = Logger.getInstance("org.hyperskill.academy.learning.framework.impl.UserChangesExtensions")

fun UserChanges.apply(project: Project, taskDir: VirtualFile, task: Task) {
  LOG.warn("UserChanges.apply: applying ${changes.size} changes to taskDir=${taskDir.path}")
  for (change in changes) {
    LOG.warn("UserChanges.apply: change=${change.javaClass.simpleName}(${change.path}, ${change.text.length} chars)")
    change.apply(project, taskDir, task)
  }
}

fun Change.apply(project: Project, taskDir: VirtualFile, task: Task) {
  when (this) {
    is Change.AddFile -> apply(project, taskDir, task)
    is Change.RemoveFile -> apply(project, taskDir, task)
    is Change.ChangeFile -> apply(project, taskDir, task)
    is Change.PropagateLearnerCreatedTaskFile -> apply(project, taskDir, task)
    is Change.RemoveTaskFile -> apply(project, taskDir, task)
  }
}

private fun Change.AddFile.apply(project: Project, taskDir: VirtualFile, task: Task) {
  if (task.getTaskFile(path) == null) {
    GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, path, InMemoryTextualContents(text))
  }
  else {
    try {
      EduDocumentListener.modifyWithoutListener(task, path) {
        GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, path, InMemoryTextualContents(text))
      }
    }
    catch (e: IOException) {
      LOG.error("Failed to create file `${taskDir.path}/$path`", e)
    }
  }
}

private fun Change.RemoveFile.apply(project: Project, taskDir: VirtualFile, task: Task) {
  runUndoTransparentWriteAction {
    try {
      taskDir.findFileByRelativePath(path)?.removeWithEmptyParents(taskDir)
    }
    catch (e: IOException) {
      LOG.error("Failed to delete file `${taskDir.path}/$path`", e)
    }
  }
}

private fun Change.ChangeFile.apply(project: Project, taskDir: VirtualFile, task: Task) {
  LOG.warn("ChangeFile.apply: path=$path, taskDir=${taskDir.path}, textLength=${text.length}")
  val file = taskDir.findFileByRelativePath(path)
  if (file == null) {
    LOG.warn("ChangeFile.apply: Can't find file `$path` in `$taskDir`")
    return
  }
  LOG.warn("ChangeFile.apply: Found file at ${file.path}")

  if (file.isToEncodeContent) {
    LOG.warn("ChangeFile.apply: Using binary content mode")
    file.doWithoutReadOnlyAttribute {
      runWriteAction {
        file.setBinaryContent(Base64.decodeBase64(text))
      }
    }
  }
  else {
    LOG.warn("ChangeFile.apply: Using document mode")
    EduDocumentListener.modifyWithoutListener(task, path) {
      val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) }
      if (document != null) {
        val expandedText = StringUtil.convertLineSeparators(EduMacroUtils.expandMacrosForFile(project.toCourseInfoHolder(), file, text))
        LOG.warn("ChangeFile.apply: Setting document text, expandedTextLength=${expandedText.length}")
        file.doWithoutReadOnlyAttribute {
          runUndoTransparentWriteAction { document.setText(expandedText) }
        }
        // ALT-10961: Force save document to disk
        FileDocumentManager.getInstance().saveDocument(document)
        LOG.warn("ChangeFile.apply: Document text set and saved successfully")
      }
      else {
        LOG.warn("ChangeFile.apply: Can't get document for `$file`")
      }
    }
  }
}

private fun Change.PropagateLearnerCreatedTaskFile.apply(project: Project, taskDir: VirtualFile, task: Task) {
  val taskFile = TaskFile(path, text).apply { isLearnerCreated = true }
  task.addTaskFile(taskFile)
}

private fun Change.RemoveTaskFile.apply(project: Project, taskDir: VirtualFile, task: Task) {
  task.removeTaskFile(path)
}
