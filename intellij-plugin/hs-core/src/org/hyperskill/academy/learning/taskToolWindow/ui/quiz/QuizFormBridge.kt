package org.hyperskill.academy.learning.taskToolWindow.ui.quiz

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.stepik.api.StepikBasedConnector
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.loadQuizFormState
import org.hyperskill.academy.learning.taskToolWindow.ui.insertBeforeBodyEnd

/**
 * Connects the quiz form inside the description page (`hyperskill_quiz.js`) to the [QuizTask] it shows.
 *
 * The form is a part of the description page, so a question and its options scroll together, like on the website.
 * The page asks for its content itself when it is loaded, so there is no need to track when it is ready to execute scripts.
 */
class QuizFormBridge(private val project: Project, private val browser: JBCefBrowserBase) : Disposable {
  private val query = JBCefJSQuery.create(browser)

  @Volatile
  private var shownTask: QuizTask? = null

  @Volatile
  private var isPageLoaded = false

  init {
    Disposer.register(this, query)
    query.addHandler { message ->
      handleMessage(message)
      null
    }
  }

  fun addFormToDescription(task: Task?, description: String): String {
    isPageLoaded = false
    shownTask = task as? QuizTask
    if (task !is QuizTask) return description

    val fragment = buildString {
      append("""<div id="hs-quiz" class="hs-quiz"></div>""")
      append("<style>:root { color-scheme: ${if (JBColor.isBright()) "light" else "dark"}; }\n$FORM_STYLE</style>")
      append("<script>$FORM_SCRIPT</script>")
      append("<script>hsQuiz.start(${task.id}, function (message) { ${query.inject("message")} });</script>")
    }
    return insertBeforeBodyEnd(description, fragment)
  }

  /**
   * Shows the actual state of the [task] in the form, if it is shown at all.
   * If the page is still being loaded, there is nothing to do: the form will ask for the content itself.
   */
  fun reloadForm(task: Task?) {
    if (task is QuizTask && task === shownTask && isPageLoaded) {
      loadForm(task)
    }
  }

  private fun handleMessage(message: String) {
    val task = shownTask ?: return
    val json = try {
      MAPPER.readTree(message)
    }
    catch (e: JsonProcessingException) {
      LOG.warn("Unexpected message from the quiz form", e)
      return
    }
    // The message could have been sent by the page of the previous task
    if (json.path("step").asInt() != task.id) return

    when (json.path("event").asText()) {
      "ready" -> {
        isPageLoaded = true
        loadForm(task)
      }

      "reply" -> task.reply = json.get("reply").toReply()
    }
  }

  private fun JsonNode?.toReply(): Map<String, Any?>? {
    if (this == null || isNull) return null
    return MAPPER.convertValue(this, object : TypeReference<Map<String, Any?>>() {})
  }

  private fun loadForm(task: QuizTask) {
    ApplicationManager.getApplication().executeOnPooledThread {
      val state = loadQuizFormState(task)
      if (project.isDisposed || shownTask !== task) return@executeOnPooledThread
      val script = "hsQuiz.render(${MAPPER.writeValueAsString(state.toMap())});"
      browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
    }
  }

  override fun dispose() {}

  companion object {
    private val LOG = logger<QuizFormBridge>()
    private val MAPPER = StepikBasedConnector.createObjectMapper(SimpleModule())

    private val FORM_SCRIPT: String by lazy { readResource("/quiz/hyperskill_quiz.js") }
    private val FORM_STYLE: String by lazy { readResource("/quiz/hyperskill_quiz.css") }

    private fun readResource(path: String): String {
      val resource = QuizFormBridge::class.java.getResource(path) ?: error("Resource $path is not found")
      return resource.readText()
    }
  }
}
