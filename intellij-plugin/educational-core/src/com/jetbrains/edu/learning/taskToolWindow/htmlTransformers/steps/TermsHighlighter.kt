package com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.steps

import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformer
import com.jetbrains.edu.learning.taskToolWindow.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskToolWindow.ui.canShowTerms
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
