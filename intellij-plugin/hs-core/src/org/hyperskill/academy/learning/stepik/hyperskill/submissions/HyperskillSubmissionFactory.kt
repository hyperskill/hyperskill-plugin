package org.hyperskill.academy.learning.stepik.hyperskill.submissions

import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.stepik.api.CodeTaskReply
import org.hyperskill.academy.learning.stepik.api.EduTaskReply
import org.hyperskill.academy.learning.stepik.api.Feedback
import org.hyperskill.academy.learning.stepik.api.StepikBasedSubmission
import org.hyperskill.academy.learning.submissions.SolutionFile

object HyperskillSubmissionFactory {
  fun createCodeTaskSubmission(attempt: Attempt, answer: String, language: String): StepikBasedSubmission {
    val reply = CodeTaskReply()
    reply.code = answer
    reply.language = language
    return StepikBasedSubmission(attempt, reply)
  }

  fun createEduTaskSubmission(task: Task, attempt: Attempt, files: List<SolutionFile>, feedback: String): StepikBasedSubmission {
    val reply = EduTaskReply()
    reply.feedback = Feedback(feedback)
    reply.score = if (task.status == CheckStatus.Solved) "1" else "0"
    reply.solution = files
    return StepikBasedSubmission(attempt, reply)
  }

  fun createRemoteEduTaskSubmission(task: RemoteEduTask, attempt: Attempt, files: List<SolutionFile>): StepikBasedSubmission {
    val reply = EduTaskReply()
    reply.checkProfile = task.checkProfile
    reply.solution = files
    return StepikBasedSubmission(attempt, reply)
  }
}