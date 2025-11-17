package org.hyperskill.academy.ai.debugger.core.connector

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.dto.FileContentMap
import com.jetbrains.educational.ml.debugger.dto.ProgrammingLanguage
import com.jetbrains.educational.ml.debugger.request.TaskDescriptionBase
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails
import okhttp3.ConnectionPool
import org.hyperskill.academy.ai.debugger.core.error.AIDebuggerServiceError
import org.hyperskill.academy.ai.debugger.core.error.BreakpointHintError
import org.hyperskill.academy.ai.debugger.core.error.BreakpointsError
import org.hyperskill.academy.ai.debugger.core.host.AIDebuggerServiceHost
import org.hyperskill.academy.ai.debugger.core.service.*
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.network.createRetrofitBuilder
import retrofit2.Response
import retrofit2.converter.jackson.JacksonConverterFactory
import java.net.HttpURLConnection.HTTP_NO_CONTENT
import java.net.HttpURLConnection.HTTP_OK

@Service(Service.Level.APP)
class AIDebuggerServiceConnector {
  private val url: String
    get() = AIDebuggerServiceHost.selectedHost.url

  private val connectionPool = ConnectionPool()

  private val service: AIDebuggerService
    get() = createAIDebuggerService()

  @Throws(IllegalStateException::class)
  private fun createAIDebuggerService(): AIDebuggerService {
    val objectMapper = jacksonObjectMapper()
    val factory = JacksonConverterFactory.create(objectMapper)
    return createRetrofitBuilder(url, connectionPool)
      .addConverterFactory(factory)
      .build()
      .create(AIDebuggerService::class.java)
  }

  suspend fun getBreakpointHint(
    files: Map<String, String>,
    finalBreakpoints: List<Breakpoint>,
    intermediateBreakpoints: List<Breakpoint>
  ): Result<List<BreakpointHintDetails>, AIDebuggerServiceError> {
    val request = BreakpointHintRequest(intermediateBreakpoints, finalBreakpoints, files)
    return service.getBreakpointHint(request).handleResponse()
  }

  suspend fun getBreakpoints(
    authorSolution: FileContentMap,
    courseId: Int,
    programmingLanguage: ProgrammingLanguage,
    task: Task,
    taskDescription: TaskDescriptionBase,
    testInfo: TestInfo,
    userSolution: FileContentMap
  ): Result<List<Breakpoint>, AIDebuggerServiceError> {
    val request = DebuggerHintRequest(
      authorSolution = authorSolution,
      courseInfo = CourseInfo(courseId),
      lessonName = task.lesson.name,
      taskName = task.name,
      programmingLanguage = programmingLanguage,
      taskDescription = taskDescription,
      taskId = task.id,
      testInfo = testInfo,
      userSolution = userSolution
    )
    return service.getBreakpoints(request).handleResponse()
  }

  private inline fun <reified T> Response<List<T>>.handleResponse(): Result<List<T>, AIDebuggerServiceError> {
    val code = code()
    val errorBody = errorBody()?.string()
    if (!errorBody.isNullOrEmpty()) {
      LOG.warn("Request failed. Status code: $code. Error message: $errorBody")
    }
    val responseBody = body()
    return when {
      code == HTTP_OK && responseBody != null -> Ok(responseBody)
      code == HTTP_NO_CONTENT -> Err(getNoContentError<T>())
      else -> Err(getDefaultError<T>())
    }
  }

  private inline fun <reified T> getNoContentError(): AIDebuggerServiceError {
    return when (T::class) {
      Breakpoint::class -> BreakpointsError.NO_BREAKPOINTS
      BreakpointHintDetails::class -> BreakpointHintError.NO_BREAKPOINT_HINTS
      else -> BreakpointsError.NO_BREAKPOINTS
    }
  }

  private inline fun <reified T> getDefaultError(): AIDebuggerServiceError {
    return when (T::class) {
      BreakpointHintDetails::class -> BreakpointHintError.DEFAULT_ERROR
      else -> BreakpointsError.DEFAULT_ERROR
    }
  }

  companion object {
    private val LOG: Logger = logger<AIDebuggerServiceConnector>()

    fun getInstance(): AIDebuggerServiceConnector = service()
  }
}