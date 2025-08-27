package org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.steps

import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.StringHtmlTransformer
import org.hyperskill.academy.learning.taskToolWindow.ui.htmlWithResources

object ResourceWrapper : StringHtmlTransformer {
  override fun transform(html: String, context: HtmlTransformerContext): String = htmlWithResources(context.project, html, context.task)
}