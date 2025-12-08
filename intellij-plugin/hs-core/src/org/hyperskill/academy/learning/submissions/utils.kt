@file:JvmName("SubmissionUtils")

package org.hyperskill.academy.learning.submissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ColorUtil
import com.intellij.util.Time
import org.hyperskill.academy.learning.RemoteEnvHelper
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.CORRECT
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.findTaskFileInDir
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.TaskToolWindowBundle
import org.hyperskill.academy.learning.ui.EduColors
import java.net.URL
import java.text.DateFormat
import java.util.*

const val OPEN_PLACEHOLDER_TAG = "<placeholder>"
const val CLOSE_PLACEHOLDER_TAG = "</placeholder>"
private const val MAX_FILE_SIZE: Int = 5 * 1024 * 1024 // 5 Mb
private val LOG: Logger = logger<Submission>()

fun getSolutionFiles(project: Project, task: Task): List<SolutionFile> {
  val files = ArrayList<SolutionFile>()
  val taskDir = task.getDir(project.courseDir) ?: error("Failed to find task directory ${task.name}")

  for (taskFile in task.taskFiles.values) {
    val virtualFile = findTaskFileInDirWithSizeCheck(taskFile, taskDir) ?: continue

    ApplicationManager.getApplication().runReadAction {
      val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
      val text = document.text
      val builder = StringBuilder(text)
      files.add(SolutionFile(taskFile.name, builder.toString(), taskFile.isVisible))
    }
  }

  return files.checkNotEmpty()
}

fun findTaskFileInDirWithSizeCheck(taskFile: TaskFile, taskDir: VirtualFile): VirtualFile? {
  val virtualFile = taskFile.findTaskFileInDir(taskDir) ?: return null
  return if (virtualFile.length > MAX_FILE_SIZE) {
    LOG.warn("File ${virtualFile.path} is too big (${virtualFile.length} bytes), will be ignored for submitting to the server")
    null
  }
  else virtualFile
}

fun List<SolutionFile>.checkNotEmpty(): List<SolutionFile> {
  if (isEmpty()) {
    error("No files were collected to post solution")
  }
  else return this
}

internal fun Date.isSignificantlyAfter(otherDate: Date): Boolean {
  val diff = time - otherDate.time
  return diff > Time.SECOND * 10
}

fun formatDate(time: Date): String {
  val calendar = GregorianCalendar()
  calendar.time = time
  val forceShowInUTC = RemoteEnvHelper.isRemoteDevServer()
  val timeStyle = if (forceShowInUTC) DateFormat.LONG else DateFormat.MEDIUM
  val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, timeStyle, Locale.getDefault())
  if (forceShowInUTC) formatter.timeZone = TimeZone.getTimeZone("UTC")
  return formatter.format(calendar.time).replace("UTC", "(UTC)")
}

fun getImageUrl(status: String?): URL? {
  val iconPath = when (status) {
    CORRECT -> if (StyleResourcesManager.isHighContrast()) "/icons/org/hyperskill/academy/submission/taskSolvedHighContrast@2x.png"
    else "/icons/org/hyperskill/academy/submission/taskSolved@2x.png"

    else -> if (StyleResourcesManager.isHighContrast()) "/icons/org/hyperskill/academy/submission/taskFailedHighContrast@2x.png"
    else "/icons/org/hyperskill/academy/submission/taskFailed@2x.png"
  }
  return Submission::class.java.getResource(iconPath)
}

fun getLinkColor(submission: Submission): String {
  return when (submission.status) {
    CORRECT -> getCorrectLinkColor()
    else -> getWrongLinkColor()
  }
}

private fun getCorrectLinkColor(): String {
  return if (StyleResourcesManager.isHighContrast()) {
    TaskToolWindowBundle.value("correct.label.foreground.high.contrast")
  }
  else {
    "#${ColorUtil.toHex(EduColors.correctLabelForeground)}"
  }
}

private fun getWrongLinkColor(): String {
  return if (StyleResourcesManager.isHighContrast()) {
    TaskToolWindowBundle.value("wrong.label.foreground.high.contrast")
  }
  else {
    "#${ColorUtil.toHex(EduColors.wrongLabelForeground)}"
  }
}