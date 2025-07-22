package org.hyperskill.academy.learning.stepik.hyperskill.checker

import org.hyperskill.academy.learning.MockResponseFactory
import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.ext.allTasks
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.pathWithoutPrams
import org.intellij.lang.annotations.Language
import org.junit.Test

class HyperskillCheckEduTaskTest : HyperskillCheckActionTestBase() {

  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      section("Topics") {
        lesson("Topic name") {
          eduTask("Problem name 1") {
            checkResultFile(CheckStatus.Solved)
          }
          eduTask("Problem name 2") {
            checkResultFile(CheckStatus.Failed)
          }
        }
      }
    }
  }

  override fun setUp() {
    super.setUp()
    configureResponse()
  }

  @Test
  fun `test solved edu task`() {
    checkCheckAction(getCourse().allTasks[0], CheckStatus.Solved, CheckUtils.CONGRATULATIONS)
  }

  @Test
  fun `test failed edu task`() {
    checkCheckAction(getCourse().allTasks[1], CheckStatus.Failed)
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