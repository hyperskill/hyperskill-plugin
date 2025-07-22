package org.hyperskill.academy.learning

import com.intellij.ide.SaveAndSyncHandler
import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.PlatformUtils
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.DescriptionFormat
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.learning.projectView.ProgressUtil.updateCourseProgress
import org.hyperskill.academy.learning.taskToolWindow.ui.EduBrowserHyperlinkListener
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.flavours.gfm.StrikeThroughDelimiterParser
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.intellij.markdown.parser.sequentialparsers.EmphasisLikeParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParser
import org.intellij.markdown.parser.sequentialparsers.SequentialParserManager
import org.intellij.markdown.parser.sequentialparsers.impl.*

object EduUtilsKt {
  fun DataContext.showPopup(htmlContent: String, position: Balloon.Position = Balloon.Position.above) {
    val balloon = JBPopupFactory.getInstance()
      .createHtmlTextBalloonBuilder(
        htmlContent,
        null,
        UIUtil.getToolTipActionBackground(),
        EduBrowserHyperlinkListener.INSTANCE
      )
      .createBalloon()

    val tooltipRelativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(this)
    balloon.show(tooltipRelativePoint, position)
  }

  fun convertToHtml(markdownText: String): String {
    // Markdown parser is supposed to work with normalized text from document
    val normalizedText = StringUtil.convertLineSeparators(markdownText)

    val flavour = EDUGFMFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(markdownText)

    return HtmlGenerator(normalizedText, parsedTree, flavour, false).generateHtml()
  }

  @Suppress("UnstableApiUsage")
  fun isAndroidStudio(): Boolean = "AndroidStudio" == PlatformUtils.getPlatformPrefix()

  fun isTaskDescriptionFile(fileName: String): Boolean = fileName matches DescriptionFormat.taskDescriptionRegex

  fun updateToolWindows(project: Project) {
    TaskToolWindowView.getInstance(project).updateTaskDescription()
    updateCourseProgress(project)
  }

  fun synchronize() {
    FileDocumentManager.getInstance().saveAllDocuments()
    SaveAndSyncHandler.getInstance().refreshOpenFiles()
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
  }

  fun isTestsFile(task: Task, path: String): Boolean {
    val configurator = task.course.configurator ?: return false
    return configurator.isTestFile(task, path)
  }

  fun Project.isNewlyCreated(): Boolean {
    val userData = getUserData(CourseProjectGenerator.EDU_PROJECT_CREATED)
    return userData != null && userData
  }

  fun getCourseModeForNewlyCreatedProject(project: Project): CourseMode? {
    if (project.isDefault || LightEdit.owns(project)) return null
    return project.guessCourseDir()?.getUserData(CourseProjectGenerator.COURSE_MODE_TO_CREATE)
  }

  fun Project.isEduProject(): Boolean {
    return StudyTaskManager.getInstance(this).course != null || getCourseModeForNewlyCreatedProject(this) != null
  }

  fun Project.isStudentProject(): Boolean {
    val course = StudyTaskManager.getInstance(this).course
    return if (course != null && course.isStudy) {
      true
    }
    else CourseMode.STUDENT == getCourseModeForNewlyCreatedProject(this)
  }

}

// org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor considers links starting
// with "^(vbscript|javascript|file|data):" unsafe and converts them into "#"
// if `useSafeLinks` is `true`
private class EDUGFMFlavourDescriptor : GFMFlavourDescriptor(false, false, false) {
  override val sequentialParserManager = object : SequentialParserManager() {
    // we to exclude MathParser() from the standard list to avoid converting MathJax expressions like '$' to <span class="math"></span>
    // and usage of the MarkdownPlugins formulas processing approach
    override fun getParserSequence(): List<SequentialParser> {
      return listOf(
        AutolinkParser(listOf(MarkdownTokenTypes.AUTOLINK, GFMTokenTypes.GFM_AUTOLINK)),
        BacktickParser(),
//        MathParser(),
        ImageParser(),
        InlineLinkParser(),
        ReferenceLinkParser(),
        EmphasisLikeParser(EmphStrongDelimiterParser(), StrikeThroughDelimiterParser())
      )
    }
  }
}
