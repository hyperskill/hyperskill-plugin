package org.hyperskill.academy.learning

import com.intellij.facet.ui.FacetDependentToolWindow
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx
import com.intellij.openapi.fileTypes.impl.DetectedByContentFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectEx
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowEP
import com.intellij.openapi.wm.ext.LibraryDependentToolWindow
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.PathUtil
import com.intellij.util.messages.Topic
import com.intellij.util.messages.Topic.BroadcastDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.isBinary
import org.hyperskill.academy.learning.courseFormat.mimeFileType
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.util.*

private val LOG = Logger.getInstance("openApiExt")

val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode
val isHeadlessEnvironment: Boolean get() = ApplicationManager.getApplication().isHeadlessEnvironment

//Extension for binary files to mark files that are needed to be encoded
//In test environment most of binary file extensions are recognized as unknown
//because the plugins for their support are not available
@VisibleForTesting
const val EDU_TEST_BIN = "edutestbin"

fun checkIsBackgroundThread() {
  check(!ApplicationManager.getApplication().isDispatchThread) {
    "Long running operation invoked on UI thread"
  }
}

fun checkIsWriteActionAllowed() {
  check(ApplicationManager.getApplication().isWriteAccessAllowed) {
    "Write action is required"
  }
}

/**
 * Invokes [runnable] asynchronously in EDT checking that [Project] is not disposed yet
 *
 * @see com.intellij.openapi.application.Application.invokeLater
 */
inline fun Project.invokeLater(modalityState: ModalityState? = null, crossinline runnable: () -> Unit) {
  if (modalityState == null) {
    ApplicationManager.getApplication().invokeLater({ runnable() }, disposed)
  }
  else {
    ApplicationManager.getApplication().invokeLater({ runnable() }, modalityState, disposed)
  }
}

/**
 * Determines if a file should be treated as binary based on its path, without accessing the file content.
 * This is useful when creating new files where the content hasn't been written yet.
 */
fun shouldEncodeFileContentByPath(path: String): Boolean {
  val name = PathUtil.getFileName(path)
  val extension = FileUtilRt.getExtension(name).lowercase()

  if (isUnitTestMode && extension == EDU_TEST_BIN) {
    return true
  }

  // Check for common binary file extensions first
  // This is important because FileType detection and MIME type detection
  // may not work correctly for files that don't exist yet
  if (isCommonBinaryExtension(extension)) {
    return true
  }

  // Try to get file type by extension/name without reading content
  val fileType = FileTypeManagerEx.getInstance().getFileTypeByExtension(extension)
  if (fileType !is UnknownFileType) {
    return fileType.isBinary
  }

  // Special case for .db files
  if (extension == "db") {
    return true
  }

  // Check for font files
  if (isFontExtension(extension)) {
    return true
  }

  // Check for git objects
  if (isGitObject(name)) {
    return true
  }

  // Use MIME type detection based on path
  // Note: this may return null for non-existent files
  val contentType = mimeFileType(path)
  return if (contentType != null) {
    isBinary(contentType)
  }
  else {
    false
  }
}

private val commonBinaryExtensions = setOf(
  // Images
  "png", "jpg", "jpeg", "gif", "bmp", "ico", "svg", "webp", "tiff", "tif",
  // Archives
  "zip", "jar", "tar", "gz", "bz2", "7z", "rar",
  // Executables
  "exe", "dll", "so", "dylib", "class",
  // Media
  "mp3", "mp4", "avi", "mov", "wav", "ogg", "flac",
  // Documents
  "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
  // Other
  "bin", "dat", "db", "sqlite"
)

private fun isCommonBinaryExtension(extension: String): Boolean {
  return commonBinaryExtensions.contains(extension.lowercase())
}

/**
 * Note: there are some unsupported cases in this method.
 * For example, some files have known file type but no extension
 */
fun toEncodeFileContent(virtualFile: VirtualFile): Boolean {
  val path = virtualFile.path

  // First try to determine by path/extension without reading content
  val byPath = shouldEncodeFileContentByPath(path)

  // If path-based detection found a known type, use it
  val extension = FileUtilRt.getExtension(PathUtil.getFileName(path))
  val fileTypeByExtension = FileTypeManagerEx.getInstance().getFileTypeByExtension(extension)
  if (fileTypeByExtension !is UnknownFileType) {
    return byPath
  }

  // Fall back to content-based detection for unknown extensions
  val fileType = FileTypeManagerEx.getInstance().getFileTypeByFile(virtualFile)
  if (fileType !is UnknownFileType && fileType !is DetectedByContentFileType) {
    return fileType.isBinary
  }

  if (fileType is DetectedByContentFileType && extension == "db") {
    /** We do encode *.db files when sending them to Stepik. When we get them back they have [DetectedByContentFileType] fileType and by
     * default this file type is not binary, so we have to forcely specify it as binary
     */
    return true
  }

  // Use the path-based result as fallback
  return byPath
}

private fun isGitObject(name: String): Boolean {
  return (name.length == 38 || name.length == 40) && name.matches(Regex("[a-z0-9]+"))
}

private val fontExtensions = setOf("ttf", "otf", "ttc", "woff", "woff2")

private fun isFontExtension(extension: String): Boolean = fontExtensions.contains(extension.lowercase())

@get:TestOnly
val Project.isLight: Boolean get() = (this as? ProjectEx)?.isLight == true

val Project.courseDir: VirtualFile
  get() {
    return guessCourseDir() ?: error("Failed to find course dir for $this")
  }

fun Project.guessCourseDir(): VirtualFile? {
  val projectDir = guessProjectDir() ?: return null
  return if (projectDir.name == Project.DIRECTORY_STORE_FOLDER) {
    projectDir.parent
  }
  else projectDir
}

val Project.selectedVirtualFile: VirtualFile? get() = FileEditorManager.getInstance(this)?.selectedFiles?.firstOrNull()

val Project.selectedTaskFile: TaskFile? get() = selectedVirtualFile?.getTaskFile(this)

val AnActionEvent.eduState: EduState?
  get() {
    val project = getData(CommonDataKeys.PROJECT) ?: return null
    val editor = getData(CommonDataKeys.HOST_EDITOR) ?: return null
    val virtualFile = getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
    val taskFile = virtualFile.getTaskFile(project) ?: return null
    return EduState(project, virtualFile, editor, taskFile)
  }

val Project.eduState: EduState?
  get() {
    val virtualFile = selectedVirtualFile ?: return null
    val taskFile = virtualFile.getTaskFile(this) ?: return null
    val editor = virtualFile.getEditor(this) ?: return null
    return EduState(this, virtualFile, editor, taskFile)
  }

val Project.course: Course? get() = StudyTaskManager.getInstance(this).course

val String.xmlEscaped: String get() = StringUtil.escapeXmlEntities(this)

val String.xmlUnescaped: String get() = StringUtil.unescapeXmlEntities(this)

inline fun <T> runReadActionInSmartMode(project: Project, crossinline runnable: () -> T): T {
  return ReadAction.nonBlocking<T> { runnable() }
    .inSmartMode(project)
    .executeSynchronously()
}

fun Document.toPsiFile(project: Project): PsiFile? {
  return PsiDocumentManager.getInstance(project).getPsiFile(this)
}

fun <T> computeUnderProgress(
  project: Project? = null,
  title: String,
  canBeCancelled: Boolean = true,
  computation: (ProgressIndicator) -> T
): T =
  ProgressManager.getInstance().run(object : Task.WithResult<T, Exception>(project, title, canBeCancelled) {
    override fun compute(indicator: ProgressIndicator): T {
      return computation(indicator)
    }
  })

fun runInBackground(project: Project? = null, title: String, canBeCancelled: Boolean = true, task: (ProgressIndicator) -> Unit) =
  ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, canBeCancelled) {
    override fun run(indicator: ProgressIndicator) = task(indicator)
  })

fun <T> withRegistryKeyOff(key: String, action: () -> T): T {
  val registryValue = Registry.get(key)
  val before = try {
    registryValue.asBoolean()
  }
  catch (e: MissingResourceException) {
    LOG.error(e)
    return action()
  }

  try {
    registryValue.setValue(false)
    return action()
  }
  finally {
    registryValue.setValue(before)
  }
}

fun <V> getInEdt(modalityState: ModalityState = ModalityState.defaultModalityState(), compute: () -> V): V {
  return runBlocking(Dispatchers.EDT + modalityState.asContextElement()) {
    compute()
  }
}

inline fun <reified L> createTopic(
  displayName: String,
  direction: BroadcastDirection = BroadcastDirection.TO_CHILDREN
): Topic<L> = Topic.create(displayName, L::class.java, direction)

private val TOOL_WINDOW_EPS = listOf(
  ToolWindowEP.EP_NAME,
  LibraryDependentToolWindow.EXTENSION_POINT_NAME,
  FacetDependentToolWindow.EXTENSION_POINT_NAME
)

fun collectToolWindowExtensions(): List<ToolWindowEP> {
  return TOOL_WINDOW_EPS.flatMap { it.extensionList }
}
