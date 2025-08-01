package org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.steps

import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import org.hyperskill.academy.learning.taskToolWindow.ui.canShowTerms
import org.jsoup.nodes.Document

/**
 * Highlights terms in an HTML document by adding dashed underline style to the occurrences of the terms.
 * Takes terms from the [TermsProjectSettings] object.
 *
 * @see HtmlTransformer
 * @see TermsProjectSettings
 */
object TermsHighlighter : HtmlTransformer {

  override fun transform(html: Document, context: HtmlTransformerContext): Document {
    val task = context.task
    val project = context.project
    if (!canShowTerms(project, task)) return html
    return html
  }

}
