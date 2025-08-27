package org.hyperskill.academy.learning.network

import okhttp3.OkHttpClient
import org.hyperskill.academy.learning.Result
import retrofit2.Call
import retrofit2.Response

interface RetrofitHelper {
  fun <T> executeCall(call: Call<T>, omitErrors: Boolean = false): Result<Response<T>, String>

  fun customizeClient(builder: OkHttpClient.Builder, baseUrl: String): OkHttpClient.Builder

  val eduToolsUserAgent: String
}