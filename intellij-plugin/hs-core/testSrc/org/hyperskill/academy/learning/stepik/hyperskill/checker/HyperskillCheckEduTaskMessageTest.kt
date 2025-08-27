package org.hyperskill.academy.learning.stepik.hyperskill.checker

import org.hyperskill.academy.learning.MockResponseFactory
import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.pathWithoutPrams
import org.intellij.lang.annotations.Language
import org.junit.Test

class HyperskillCheckEduTaskMessageTest : HyperskillCheckActionTestBase() {

  override fun createCourse() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask(stepId = 1) {
          checkResultFile(CheckStatus.Solved)
        }
      }
      section("Topics") {
        lesson("Topic name") {
          eduTask("Problem name 1") {
            checkResultFile(CheckStatus.Solved)
          }
          eduTask("Problem name 2") {
            checkResultFile(CheckStatus.Solved)
          }
        }
      }
    } as HyperskillCourse
    course.stages = listOf(HyperskillStage(1, "", 1))
    course.hyperskillProject = HyperskillProject()
  }

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  @Test
  fun `test solve all edu tasks in topic`() {
    val course = getCourse() as HyperskillCourse
    checkCheckAction(
      course.getProjectLesson()!!.taskList[0],
      CheckStatus.Solved,
      EduCoreBundle.message("hyperskill.next.project", HYPERSKILL_PROJECTS_URL)
    )

    val topic = course.getTopicsSection()!!.lessons[0]
    checkCheckAction(topic.taskList[0], CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
    checkCheckAction(topic.taskList[1], CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
  }

  private fun configureResponse() {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      MockResponseFactory.fromString(
        when (val path = request.pathWithoutPrams) {
          "/api/attempts" -> attempt
          "/api/submissions" -> submission
          else -> error("Wrong path: $path")
        }
      )
    }
  }

  @Language("JSON")
  private val attempt = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 7565800,
          "status": "active",
          "step": 4368,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """

  @Language("JSON")
  private val submission = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "7565800",
          "id": "7565003",
          "status": "evaluation",
          "step": 4368,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """
}