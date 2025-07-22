package org.hyperskill.academy.learning.stepik.hyperskill.checker

import org.hyperskill.academy.learning.stepik.api.StepikBasedSubmission

sealed class SubmissionError(val error: String) {
  class NoSubmission(error: String) : SubmissionError(error)
  class WithSubmission(val submission: StepikBasedSubmission, error: String) : SubmissionError(error)
}