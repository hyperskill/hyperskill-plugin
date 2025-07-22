package org.hyperskill.academy.learning.handlers

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.FileCheck
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.findTask

abstract class VirtualFileListenerTestBase : EduTestCase() {
  protected abstract val courseMode: CourseMode
  protected abstract fun createListener(project: Project): EduVirtualFileListener

  override fun setUp() {
    super.setUp()
    ApplicationManager.getApplication().messageBus
      .connect(testRootDisposable)
      .subscribe(VirtualFileManager.VFS_CHANGES, createListener(project))
  }

  protected fun doAddFileTest(filePathInTask: String, text: String = "", checksProducer: (Task) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage,
      createYamlConfigs = true
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/Task.kt")
        }
      }
    }

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find directory of `${task.name}` task")

    GeneratorUtils.createChildFile(project, taskDir, filePathInTask, text)
    checksProducer(task).forEach(FileCheck::check)
  }

  /**
   * This is a simple copy method, similar to [VfsUtil.copyFile], [VfsUtil.copy].
   * To copy files, these methods first create a new file and then fill its contents.
   * So no [VFileCopyEvent] is fired, instead, [VFileCreateEvent] and [VFileContentChangeEvent] are called.
   * This [copy] method calls the [VirtualFile.copy], so it is theoretically possible to have the [VFileCopyEvent].
   * Unfortunately, the file system used for tests is non-real, and it indirectly calls [VfsUtil.copyFile] to copy files.
   */
  protected fun copy(requestor: Any?, file: VirtualFile, newParent: VirtualFile, newName: String) {
    @Suppress("UnsafeVfsRecursion")
    if (file.isDirectory) {
      val copiedDirectory = newParent.createChildDirectory(requestor, newName)
      for (child in file.children) {
        copy(requestor, child, copiedDirectory, child.name)
      }
    }
    else
      file.copy(requestor, newParent, newName)
  }
}
