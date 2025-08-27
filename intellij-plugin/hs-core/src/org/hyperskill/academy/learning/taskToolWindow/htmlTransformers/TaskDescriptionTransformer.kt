package org.hyperskill.academy.learning.taskToolWindow.htmlTransformers

import org.hyperskill.academy.learning.taskToolWindow.htmlTransformers.steps.*

private val TaskDescriptionHtmlTransformer = HtmlTransformer.pipeline(
  CutOutHeaderTransformer,
  CssHtmlTransformer,
  MediaThemesTransformer,
  CodeHighlighter,
  HintsWrapper,
  TermsHighlighter
)

val TaskDescriptionTransformer = StringHtmlTransformer.pipeline(
  TaskDescriptionHtmlTransformer.toStringTransformer(),
  ResourceWrapper
)