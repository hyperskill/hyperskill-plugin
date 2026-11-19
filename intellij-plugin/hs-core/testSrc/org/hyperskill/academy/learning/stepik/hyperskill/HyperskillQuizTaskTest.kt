package org.hyperskill.academy.learning.stepik.hyperskill

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.vfs.VfsUtilCore
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.MockResponseFactory
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.courseFormat.tasks.UnsupportedTask
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.pathWithoutPrams
import org.hyperskill.academy.learning.stepik.HtmlStepOptions
import org.hyperskill.academy.learning.stepik.Step
import org.hyperskill.academy.learning.stepik.api.StepikBasedConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillStepSource
import org.hyperskill.academy.learning.stepik.hyperskill.api.MockHyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillTaskBuilder
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.downloadDataset
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.loadQuizFormState
import org.hyperskill.academy.learning.yaml.YamlDeserializer
import org.hyperskill.academy.learning.yaml.YamlMapper
import org.junit.Test
import java.util.*

class HyperskillQuizTaskTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  private var postedAttempts = 0

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    try {
      logOutFakeHyperskillUser()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test unchecked quiz continues the active attempt`() {
    val task = createQuizTask()
    configureResponses(attempts = listOf(attempt(21, "outdated"), attempt(22, "active")))

    val state = loadQuizFormState(task)

    assertEquals(22, task.attempt?.id)
    assertEquals(0, postedAttempts)
    assertFalse(state.isDisabled)
    assertNull(state.error)
    assertEquals(listOf("a", "b"), state.dataset?.options)
    assertTrue(state.dataset?.isMultipleChoice == true)
  }

  @Test
  fun `test unchecked quiz without an active attempt starts a new one`() {
    val task = createQuizTask()
    configureResponses(attempts = listOf(attempt(21, "outdated")))

    loadQuizFormState(task)

    assertEquals(1, postedAttempts)
    assertEquals(NEW_ATTEMPT_ID, task.attempt?.id)
  }

  @Test
  fun `test failed quiz shows the answer of the last submission until a retry`() {
    val task = createQuizTask()
    task.status = CheckStatus.Failed
    configureResponses(
      attempts = listOf(attempt(21, "outdated"), attempt(22, "active")),
      submissions = listOf(submission(30, 22, "wrong", """{"choices": [false, true]}"""), submission(31, 21, "wrong", """{"choices": [true, false]}"""))
    )

    val state = loadQuizFormState(task)

    assertEquals(0, postedAttempts)
    assertEquals(21, task.attempt?.id)
    assertTrue(state.isDisabled)
    assertEquals(mapOf("choices" to listOf(true, false)), state.reply)
    assertEquals(state.reply, task.reply)
  }

  @Test
  fun `test solved quiz does not start a new attempt`() {
    val task = createQuizTask()
    task.status = CheckStatus.Solved
    configureResponses(
      attempts = listOf(attempt(21, "outdated")),
      submissions = listOf(submission(31, 21, "correct", """{"ordering": [1, 0]}"""))
    )

    val state = loadQuizFormState(task)

    assertEquals(0, postedAttempts)
    assertTrue(state.isDisabled)
    assertEquals(mapOf("ordering" to listOf(1, 0)), state.reply)
  }

  @Test
  fun `test answer of another attempt is not restored`() {
    val task = createQuizTask()
    configureResponses(
      attempts = listOf(attempt(22, "active")),
      submissions = listOf(submission(31, 21, "wrong", """{"choices": [true, false]}"""))
    )

    val state = loadQuizFormState(task)

    assertNull(state.reply)
    assertNull(task.reply)
  }

  @Test
  fun `test form shows an error without login`() {
    val task = createQuizTask()
    logOutFakeHyperskillUser()

    val state = loadQuizFormState(task)

    assertNotNull(state.error)
    assertEquals(stepLink(STEP_ID), state.stepLink)
    assertNull(task.attempt)
  }

  @Test
  fun `test form gets the dataset in the format of the server`() {
    val task = createQuizTask()
    val dataset = """
      {
        "pairs": [{"first": "f", "second": "s"}],
        "lines": ["line"],
        "rows": ["row"],
        "columns": ["column"],
        "is_checkbox": true,
        "components": [{"type": "text", "text": "▭"}, {"type": "select", "options": ["o"]}]
      }
    """
    configureResponses(attempts = listOf(attempt(22, "active", dataset)))

    val state = loadQuizFormState(task)

    val json = StepikBasedConnector.createObjectMapper(SimpleModule()).valueToTree<JsonNode>(state.toMap())
    assertEquals(task.blockName, json["blockName"].asText())
    assertEquals("s", json["dataset"]["pairs"][0]["second"].asText())
    assertEquals("line", json["dataset"]["lines"][0].asText())
    assertEquals("row", json["dataset"]["rows"][0].asText())
    assertEquals("column", json["dataset"]["columns"][0].asText())
    assertTrue(json["dataset"]["is_checkbox"].asBoolean())
    assertEquals("▭", json["dataset"]["components"][0]["text"].asText())
    assertEquals("o", json["dataset"]["components"][1]["options"][0].asText())
  }

  @Test
  fun `test quiz blocks become quiz tasks`() {
    val course = createCourseWithQuiz()

    for (blockName in QuizTask.BLOCK_NAMES) {
      val task = HyperskillTaskBuilder(course, stepSource(blockName)).build()
      assertInstanceOf(task, QuizTask::class.java)
      assertEquals(blockName, task?.itemType)
      assertEquals("Step title", task?.name)
      assertTrue(task!!.descriptionText.contains("The question"))
    }
  }

  @Test
  fun `test video steps stay unsupported`() {
    val course = createCourseWithQuiz()

    assertInstanceOf(HyperskillTaskBuilder(course, stepSource("video")).build(), UnsupportedTask::class.java)
  }

  @Test
  fun `test html step is answered with the page files made from its templates`() {
    val course = createCourseWithQuiz()
    val stepSource = stepSource("html")
    stepSource.block?.options = HtmlStepOptions().apply {
      htmlTemplate = "<p>Hello</p>"
      cssTemplate = "p { color: red; }"
    }

    val task = HyperskillTaskBuilder(course, stepSource).build() as QuizTask

    assertEquals(listOf(QuizTask.HTML_FILE_NAME, QuizTask.CSS_FILE_NAME), task.taskFiles.keys.toList())
    assertEquals("<p>Hello</p>", task.getTaskFile(QuizTask.HTML_FILE_NAME)?.text)
    assertEquals("p { color: red; }", task.getTaskFile(QuizTask.CSS_FILE_NAME)?.text)
  }

  @Test
  fun `test sql step is answered with a query file`() {
    val task = HyperskillTaskBuilder(createCourseWithQuiz(), stepSource("sql")).build() as QuizTask

    assertEquals(listOf(QuizTask.SQL_FILE_NAME), task.taskFiles.keys.toList())
    assertEquals("", task.getTaskFile(QuizTask.SQL_FILE_NAME)?.text)
  }

  @Test
  fun `test dataset step does not start the timer when it is opened`() {
    val task = createQuizTask(blockName = "dataset")
    configureResponses(attempts = emptyList(), stepOptions = """{"time_limit": 300, "samples": [["1 2", "3"]]}""")

    val state = loadQuizFormState(task)

    assertEquals(0, postedAttempts)
    assertNull(task.attempt)
    assertFalse(state.hasAttempt)
    assertNull(state.error)
    assertEquals(300, state.options["time_limit"])
  }

  @Test
  fun `test dataset step continues the attempt with the time that is left`() {
    val task = createQuizTask(blockName = "dataset")
    configureResponses(
      attempts = listOf(attempt(22, "active", dataset = """{"file": ""}""", timeLeft = 120)),
      stepOptions = """{"time_limit": 300}"""
    )

    val state = loadQuizFormState(task)

    assertEquals(0, postedAttempts)
    assertEquals(22, task.attempt?.id)
    assertTrue(state.hasAttempt)
    assertEquals(120L, state.timeLeft)
  }

  @Test
  fun `test html form gets the checklist of the step`() {
    val task = createQuizTask(blockName = "html")
    val checklist = """[{"name": "Has a title", "selector": "h1", "tests": [{"type": "hasElement", "data": {}}]}]"""
    configureResponses(
      attempts = listOf(attempt(22, "active", dataset = "{}")),
      stepOptions = """{"html_template": "", "css_template": "", "checklist": $checklist}"""
    )

    val state = loadQuizFormState(task)

    val json = StepikBasedConnector.createObjectMapper(SimpleModule()).valueToTree<JsonNode>(state.toMap())
    val check = json["options"]["checklist"][0]
    assertEquals("Has a title", check["name"].asText())
    assertEquals("h1", check["selector"].asText())
    assertEquals("hasElement", check["tests"][0]["type"].asText())
  }

  @Test
  fun `test dataset is downloaded next to the task files`() {
    val task = createQuizTask(blockName = "dataset")
    configureResponses(attempts = emptyList(), dataset = "1 2 3\n")

    val file = downloadDataset(project, task, startNew = false).onError { error("Failed to download the dataset: $it") }

    assertEquals(1, postedAttempts)
    assertEquals(NEW_ATTEMPT_ID, task.attempt?.id)
    assertEquals(QuizTask.DATASET_FILE_NAME, file.name)
    assertEquals(task.getDir(project.courseDir), file.parent)
    assertEquals("1 2 3\n", VfsUtilCore.loadText(file))
  }

  @Test
  fun `test dataset of the running attempt is downloaded again without a new attempt`() {
    val task = createQuizTask(blockName = "dataset")
    configureResponses(attempts = emptyList(), dataset = "4 5 6\n")
    task.attempt = Attempt(STEP_ID).apply {
      id = NEW_ATTEMPT_ID
      timeLeft = 60
    }
    task.reply = mapOf("file" to "15")

    downloadDataset(project, task, startNew = false).onError { error("Failed to download the dataset: $it") }

    assertEquals(0, postedAttempts)
    assertEquals(mapOf("file" to "15"), task.reply)
  }

  @Test
  fun `test new dataset starts a new attempt and forgets the answer`() {
    val task = createQuizTask(blockName = "dataset")
    configureResponses(attempts = emptyList(), dataset = "7 8 9\n")
    task.attempt = Attempt(STEP_ID).apply {
      id = 21
      timeLeft = 0
    }
    task.reply = mapOf("file" to "15")

    downloadDataset(project, task, startNew = true).onError { error("Failed to download the dataset: $it") }

    assertEquals(1, postedAttempts)
    assertEquals(NEW_ATTEMPT_ID, task.attempt?.id)
    assertNull(task.reply)
  }

  @Test
  fun `test only the block type and the status of a quiz are stored`() {
    val task = QuizTask("quiz", 1, 1, Date(0), CheckStatus.Failed, "matching")
    task.reply = mapOf("ordering" to listOf(0, 1))

    val mapper = YamlMapper.basicMapper()
    val yaml = mapper.writeValueAsString(task)

    assertTrue(yaml, yaml.contains("type: matching"))
    assertFalse(yaml, yaml.contains("ordering"))

    val restored = with(YamlDeserializer) { mapper.deserializeTask(yaml) }
    assertInstanceOf(restored, QuizTask::class.java)
    assertEquals("matching", (restored as QuizTask).blockName)
    assertEquals(CheckStatus.Failed, restored.status)
    assertNull(restored.reply)
  }

  private fun createCourseWithQuiz(blockName: String = "choice"): HyperskillCourse = hyperskillCourseWithFiles(projectId = null) {
    section(HYPERSKILL_TOPICS) {
      lesson("Topic") {
        quizTask("Quiz", blockName = blockName, stepId = STEP_ID) {
          taskFile("Task.txt", "")
        }
      }
    }
  }

  private fun createQuizTask(blockName: String = "choice"): QuizTask = createCourseWithQuiz(blockName).getProblem(STEP_ID) as QuizTask

  private fun stepSource(blockName: String) = HyperskillStepSource().apply {
    id = STEP_ID
    title = "Step title"
    block = Step().apply {
      name = blockName
      text = "<p>The question</p>"
    }
  }

  private fun configureResponses(
    attempts: List<String>,
    submissions: List<String> = emptyList(),
    stepOptions: String? = null,
    dataset: String? = null
  ) {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      when (request.pathWithoutPrams) {
        "/api/attempts" -> {
          if (request.method == "POST") {
            postedAttempts++
            MockResponseFactory.fromString(list("attempts", listOf(attempt(NEW_ATTEMPT_ID, "active"))))
          }
          else {
            MockResponseFactory.fromString(list("attempts", attempts))
          }
        }

        "/api/attempts/$NEW_ATTEMPT_ID/dataset" -> dataset?.let { MockResponseFactory.fromString(it) }
        "/api/submissions" -> MockResponseFactory.fromString(list("submissions", submissions))
        "/api/steps" -> stepOptions?.let { MockResponseFactory.fromString(list("steps", listOf(step(it)))) }
        else -> null
      }
    }
  }

  private fun step(options: String): String {
    val task = (getCourse() as HyperskillCourse).getProblem(STEP_ID) as QuizTask
    return """{"id": $STEP_ID, "title": "Step", "position": 1, "block": {"name": "${task.blockName}", "text": "", "options": $options}}"""
  }

  private fun list(name: String, items: List<String>): String =
    """{"meta": {"page": 1, "has_next": false, "has_previous": false}, "$name": [${items.joinToString(",")}]}"""

  private fun attempt(
    id: Int,
    status: String,
    dataset: String = """{"is_multiple_choice": true, "options": ["a", "b"]}""",
    timeLeft: Int? = null
  ): String =
    """{"id": $id, "status": "$status", "step": $STEP_ID, "time": "2026-01-01T00:00:00Z", "time_left": $timeLeft, "dataset": $dataset}"""

  private fun submission(id: Int, attempt: Int, status: String, reply: String): String =
    """{"id": $id, "attempt": $attempt, "status": "$status", "step": $STEP_ID, "time": "2026-01-01T00:00:00Z", "reply": $reply}"""

  companion object {
    private const val STEP_ID = 4
    private const val NEW_ATTEMPT_ID = 23
  }
}
