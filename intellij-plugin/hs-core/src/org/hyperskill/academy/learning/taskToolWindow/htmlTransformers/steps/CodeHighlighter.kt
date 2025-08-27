package org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.steps

import com.intellij.openapi.fileTypes.PlainTextLanguage
import org.hyperskill.academy.learning.courseFormat.ext.languageById
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import org.hyperskill.academy.learning.taskToolWindow.ui.EduCodeHighlighter.Companion.highlightCodeFragments
import org.jsoup.nodes.Document

object CodeHighlighter : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val task = context.task
    val project = context.project

    val course = task.course
    val language = if (course is HyperskillCourse) PlainTextLanguage.INSTANCE else course.languageById ?: return html

    return highlightCodeFragments(project, html, language)
  }
}