package org.hyperskill.academy.learning.taskToolWindow.ui.quiz

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import com.intellij.util.Alarm
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTaskType
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.stepik.api.StepikBasedConnector
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.QuizForm
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.QuizFormService
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.downloadDataset
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.loadQuizFormState
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.readHtmlSources
import org.hyperskill.academy.learning.taskToolWindow.ui.insertBeforeBodyEnd
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Connects the quiz form inside the description page (`hyperskill_quiz.js`) to the [QuizTask] it shows.
 *
 * The form is a part of the description page, so a question and its options scroll together, like on the website.
 * The page asks for its content itself when it is loaded, so there is no need to track when it is ready to execute scripts.
 *
 * An `html` step is answered with its task files: the form shows the page made of them, and runs its checklist when the
 * task is checked, see [QuizForm.score].
 */
class QuizFormBridge(private val project: Project, private val browser: JBCefBrowserBase) : Disposable, QuizForm {
  private val query = JBCefJSQuery.create(browser)
  private val sourcesUpdate = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

  @Volatile
  private var shownTask: QuizTask? = null

  @Volatile
  private var isPageLoaded = false

  /** The files of the shown `html` task, to update its page when they change. */
  @Volatile
  private var htmlFiles: Set<VirtualFile> = emptySet()

  @Volatile
  private var pendingScore: PendingScore? = null

  init {
    Disposer.register(this, query)
    query.addHandler { message ->
      handleMessage(message)
      null
    }
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        val file = FileDocumentManager.getInstance().getFile(event.document) ?: return
        if (file in htmlFiles) {
          sourcesUpdate.cancelAllRequests()
          sourcesUpdate.addRequest(::updateHtmlSources, SOURCES_UPDATE_DELAY_MS)
        }
      }
    }, this)
    QuizFormService.getInstance(project).form = this
  }

  fun addFormToDescription(task: Task?, description: String): String {
    isPageLoaded = false
    htmlFiles = emptySet()
    shownTask = task as? QuizTask
    if (task !is QuizTask) return description

    val fragment = buildString {
      append("""<div id="hs-quiz" class="hs-quiz"></div>""")
      append("<style>:root { color-scheme: ${if (JBColor.isBright()) "light" else "dark"}; }\n$FORM_STYLE</style>")
      if (task.isHtml) {
        for (script in HTML_CHECKLIST_SCRIPTS) {
          append(inlineScript(script))
        }
      }
      append(inlineScript(FORM_SCRIPT))
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

  override fun score(task: QuizTask, sources: Map<String, String>): Any? {
    if (task !== shownTask || !isPageLoaded) return null

    val pending = PendingScore(task.id, sources)
    pendingScore = pending
    executeJavaScript("hsQuiz.checkHtml(${MAPPER.writeValueAsString(sources)});")
    return try {
      pending.score.get(SCORE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }
    catch (_: TimeoutException) {
      LOG.warn("The checklist of the step ${task.id} hasn't been run in $SCORE_TIMEOUT_SECONDS seconds")
      null
    }
    finally {
      if (pendingScore === pending) pendingScore = null
    }
  }

  override fun focus(task: QuizTask) {
    if (task !== shownTask || !isPageLoaded) return
    val component = browser.component ?: return
    IdeFocusManager.getInstance(project).requestFocus(component, true)
    executeJavaScript("hsQuiz.focus();")
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

      "reply" -> task.reply = json.get("reply").toMap()
      "download" -> downloadDatasetAndReload(task, startNew = json.path("restart").asBoolean())
      "score" -> {
        val pending = pendingScore ?: return
        val sources = json.get("sources").toMap()
        if (pending.taskId == task.id && sources == pending.sources) {
          pending.score.complete(json.get("score")?.numberValue())
        }
      }
    }
  }

  private fun JsonNode?.toMap(): Map<String, Any?>? {
    if (this == null || isNull) return null
    return MAPPER.convertValue(this, object : TypeReference<Map<String, Any?>>() {})
  }

  private fun loadForm(task: QuizTask) {
    ApplicationManager.getApplication().executeOnPooledThread {
      var state = loadQuizFormState(task)
      if (task.isHtml && state.error == null) {
        htmlFiles = QuizTask.ANSWER_FILES[task.blockName].orEmpty().mapNotNull { task.getTaskFile(it)?.getVirtualFile(project) }.toSet()
        state = state.withSources(readHtmlSources(project, task).orEmpty())
      }
      if (project.isDisposed || shownTask !== task) return@executeOnPooledThread
      executeJavaScript("hsQuiz.render(${MAPPER.writeValueAsString(state.toMap())});")
    }
  }

  private fun updateHtmlSources() {
    val task = shownTask ?: return
    if (!task.isHtml || !isPageLoaded || project.isDisposed) return
    val sources = readHtmlSources(project, task) ?: return
    executeJavaScript("hsQuiz.updateSources(${MAPPER.writeValueAsString(sources)});")
  }

  private fun downloadDatasetAndReload(task: QuizTask, startNew: Boolean) {
    ApplicationManager.getApplication().executeOnPooledThread {
      val file = downloadDataset(project, task, startNew).onError { error ->
        LOG.warn("Failed to download the dataset of the step ${task.id}: $error")
        EduNotificationManager.showErrorNotification(project, content = EduCoreBundle.message("hyperskill.dataset.failed.to.download"))
        null
      }
      if (file != null) {
        runInEdt {
          if (!project.isDisposed) FileEditorManager.getInstance(project).openFile(file, false)
        }
      }
      reloadForm(task)
    }
  }

  private fun executeJavaScript(script: String) {
    browser.cefBrowser.executeJavaScript(script, browser.cefBrowser.url, 0)
  }

  override fun dispose() {
    val service = QuizFormService.getInstance(project)
    if (service.form === this) service.form = null
  }

  private class PendingScore(val taskId: Int, val sources: Map<String, String>) {
    val score: CompletableFuture<Any?> = CompletableFuture()
  }

  companion object {
    private val LOG = logger<QuizFormBridge>()
    private val MAPPER = StepikBasedConnector.createObjectMapper(SimpleModule())

    private const val SOURCES_UPDATE_DELAY_MS = 300
    private const val SCORE_TIMEOUT_SECONDS = 10L

    private val FORM_SCRIPT: String by lazy { readResource("/quiz/hyperskill_quiz.js") }
    private val FORM_STYLE: String by lazy { readResource("/quiz/hyperskill_quiz.css") }

    /** The checklist of an `html` step and the libraries it needs, see `quiz/lib/README.md`. */
    private val HTML_CHECKLIST_SCRIPTS: List<String> by lazy {
      listOf(
        "/quiz/lib/htmlhint.min.js",
        "/quiz/lib/csstree-validator.min.js",
        "/quiz/lib/css-shorthand-properties.js",
        "/quiz/hyperskill_html_tests.js",
      ).map(::readResource)
    }

    private val QuizTask.isHtml: Boolean
      get() = blockName == HyperskillTaskType.HTML.type

    /**
     * A script put right into the page. `</script` inside it, e.g. in a string of the HTML linter, would end the element,
     * and `<\/script` means the same in JavaScript code.
     */
    private fun inlineScript(code: String): String = "<script>${code.replace("</script", "<\\/script", ignoreCase = true)}</script>"

    private fun readResource(path: String): String {
      val resource = QuizFormBridge::class.java.getResource(path) ?: error("Resource $path is not found")
      return resource.readText()
    }
  }
}
