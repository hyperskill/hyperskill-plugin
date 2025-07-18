package com.jetbrains.edu.learning.exceptions

import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import org.jetbrains.annotations.Nls

class BrokenPlaceholderException(
  @Nls(capitalization = Nls.Capitalization.Sentence) override val message: String,
  val placeholder: AnswerPlaceholder
) : IllegalStateException() 