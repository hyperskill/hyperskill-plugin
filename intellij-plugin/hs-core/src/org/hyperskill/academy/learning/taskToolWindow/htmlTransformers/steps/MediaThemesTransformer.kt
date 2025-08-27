package org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.steps

import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import org.hyperskill.academy.learning.taskToolWindow.replaceMediaForTheme
import org.jsoup.nodes.Document

object MediaThemesTransformer : HtmlTransformer {
  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val task = context.task
    return replaceMediaForTheme(context.project, task, html)
  }
}