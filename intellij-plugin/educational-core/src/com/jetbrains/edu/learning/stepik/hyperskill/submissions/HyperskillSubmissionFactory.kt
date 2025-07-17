package com.jetbrains.edu.learning.stepik.hyperskill.submissions

import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.api.CodeTaskReply
import com.jetbrains.edu.learning.stepik.api.EduTaskReply
import com.jetbrains.edu.learning.stepik.api.Feedback
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.submissions.SolutionFile

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