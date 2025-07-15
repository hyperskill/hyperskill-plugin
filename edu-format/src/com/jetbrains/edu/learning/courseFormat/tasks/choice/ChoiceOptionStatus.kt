package com.jetbrains.edu.learning.courseFormat.tasks.choice

import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus.UNKNOWN


/**
 * Choice tasks created on Stepik can't be checked locally, so they have [UNKNOWN] status
 */
enum class ChoiceOptionStatus {
  CORRECT, INCORRECT, UNKNOWN
}