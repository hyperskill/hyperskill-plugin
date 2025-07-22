package org.hyperskill.academy.learning.courseFormat

import org.hyperskill.academy.learning.courseFormat.EduFormatNames.CORRECT
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.UNCHECKED
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.WRONG

enum class CheckStatus(val rawStatus: String) {
  Unchecked(UNCHECKED),
  Solved(CORRECT),
  Failed(WRONG);

  companion object {
    fun String.toCheckStatus(): CheckStatus = when (this) {
      CORRECT -> Solved
      WRONG -> Failed
      else -> Unchecked
    }
  }
}