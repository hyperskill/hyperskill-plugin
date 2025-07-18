package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checker.CheckUtils.CONGRATULATIONS
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask
import com.jetbrains.edu.learning.stepik.api.StepikBasedSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createCodeTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createEduTaskSubmission
import com.jetbrains.edu.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory.createRemoteEduTaskSubmission
import com.jetbrains.edu.learning.submissions.getSolutionFiles
import com.jetbrains.edu.learning.yaml.YamlMapper
import org.junit.Test

class HyperskillCreateSubmissionTest : EduTestCase() {
  private val hyperskillCourse: HyperskillCourse by lazy {
    courseWithFiles(language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson("Topic name") {
          eduTask("Edu problem", stepId = 1) {
            taskFile("src/Task.kt")
            taskFile("src/Test.kt", visible = false)
          }
          remoteEduTask("Remote Edu problem", stepId = 2, checkProfile = "hyperskill_go") {
            taskFile("src/Task.kt")
            taskFile("src/Test.kt", visible = false)
          }
        }
      }
    } as HyperskillCourse
  }

  @Test
  fun `test creating submission for solved edu task`() {
    val eduTask = hyperskillCourse.allTasks[0].apply { status = CheckStatus.Solved }
    val attempt = Attempt().apply { id = 123 }
    val solutionFiles = getSolutionFiles(project, eduTask)
    val feedback = CONGRATULATIONS
    val submission = createEduTaskSubmission(eduTask, attempt, solutionFiles, feedback)

    doTest(
      submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  feedback:
      |    message: $feedback
      |  score: 1
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test creating submission for failed edu task`() {
    val eduTask = hyperskillCourse.allTasks[0].apply { status = CheckStatus.Failed }
    val attempt = Attempt().apply { id = 1234 }
    val solutionFiles = getSolutionFiles(project, eduTask)
    val feedback = "failed"
    val submission = createEduTaskSubmission(eduTask, attempt, solutionFiles, feedback)

    doTest(
      submission, """
      |attempt: 1234
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  feedback:
      |    message: $feedback
      |  score: 0
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test creating submission for remote edu task`() {
    val remoteEduTask = hyperskillCourse.allTasks[1] as RemoteEduTask
    val checkProfile = remoteEduTask.checkProfile
    val attempt = Attempt().apply { id = 12345 }
    val solutionFiles = getSolutionFiles(project, remoteEduTask)
    val submission = createRemoteEduTaskSubmission(remoteEduTask, attempt, solutionFiles)

    doTest(
      submission, """
      |attempt: 12345
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  solution:
      |  - name: src/Task.kt
      |    is_visible: true
      |  - name: src/Test.kt
      |    is_visible: false
      |  check_profile: $checkProfile
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test creating submission for code task`() {
    val attempt = Attempt().apply { id = 123 }
    val answer = "answer"
    val language = "language"
    val submission = createCodeTaskSubmission(attempt, answer, language)

    doTest(
      submission, """
      |attempt: 123
      |reply:
      |  version: $JSON_FORMAT_VERSION
      |  language: $language
      |  code: $answer
      |
    """.trimMargin()
    )
  }

  private fun doTest(submission: StepikBasedSubmission, expected: String) {
    val actual = YamlMapper.basicMapper().writeValueAsString(submission)
    assertEquals(expected, actual)
  }
}