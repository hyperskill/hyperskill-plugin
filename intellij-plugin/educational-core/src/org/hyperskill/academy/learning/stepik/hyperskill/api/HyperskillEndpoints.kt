package org.hyperskill.academy.learning.stepik.hyperskill.api

import okhttp3.ResponseBody
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.stepik.StepikNames
import org.hyperskill.academy.learning.stepik.api.AttemptsList
import org.hyperskill.academy.learning.stepik.api.CourseAdditionalInfo
import org.hyperskill.academy.learning.stepik.api.StepikBasedSubmission
import org.hyperskill.academy.learning.stepik.api.SubmissionsList
import retrofit2.Call
import retrofit2.http.*

interface HyperskillEndpoints {
  @GET("api/profiles/current")
  fun getCurrentUserInfo(): Call<ProfilesList>

  @GET("api/stages")
  fun stages(@Query("project") projectId: Int, @Query("page") page: Int, @Query("page_size") pageSize: Int = 100): Call<StagesList>

  @GET("api/topics")
  fun topics(@Query("stage") stageId: Int, @Query("page") page: Int, @Query("page_size") pageSize: Int = 100): Call<TopicsList>

  @GET("api/steps")
  fun steps(
    @Query("ids", encoded = true) ids: String,
    @Query("page") page: Int,
    @Query("page_size") pageSize: Int = 100
  ): Call<HyperskillStepsList>

  @GET("api/steps")
  fun steps(@Query("topic") topic: Int, @Query("page") page: Int, @Query("page_size") pageSize: Int = 100): Call<HyperskillStepsList>

  @GET("api/steps")
  fun steps(@Query("ids", encoded = true) ids: String): Call<HyperskillStepsList>

  @GET("api/projects/{id}/additional-files/${StepikNames.ADDITIONAL_INFO}")
  fun additionalFiles(@Path("id") id: Int): Call<CourseAdditionalInfo>

  @GET("api/submissions")
  fun submissions(
    @Query("user") user: Int,
    @Query("step", encoded = true) step: String,
    @Query("page") page: Int,
    @Query("page_size") pageSize: Int = 100
  ): Call<SubmissionsList>

  @GET("api/submissions/{id}")
  fun submission(@Path("id") submissionId: Int): Call<SubmissionsList>

  @GET("api/projects/{id}")
  fun project(@Path("id") projectId: Int): Call<ProjectsList>

  @GET("api/users/{id}")
  fun user(@Path("id") id: Int): Call<UsersList>

  @GET("api/attempts")
  fun attempts(
    @Query("step") stepId: Int,
    @Query("user") userId: Int,
    @Query("page") page: Int,
    @Query("page_size") pageSize: Int = 100
  ): Call<AttemptsList>

  @GET("api/attempts/{dataset_id}/dataset")
  fun dataset(@Path("dataset_id") datasetId: Int): Call<ResponseBody>

  @POST("api/attempts")
  fun attempt(@Body attempt: Attempt): Call<AttemptsList>

  @POST("api/submissions")
  fun submission(@Body submission: StepikBasedSubmission): Call<SubmissionsList>

  @POST("api/ws")
  fun websocket(): Call<WebSocketConfiguration>

  @POST("/api/frontend-events")
  fun sendFrontendEvents(@Body events: List<HyperskillFrontendEvent>): Call<Any>

  @POST("/api/time-spent-events")
  fun sendTimeSpentEvents(@Body events: List<HyperskillTimeSpentEvent>): Call<Any>
}
