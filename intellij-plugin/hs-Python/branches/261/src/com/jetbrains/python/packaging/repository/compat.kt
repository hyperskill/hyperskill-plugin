package com.jetbrains.python.packaging.repository

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

internal fun encodeCredentialsForUrl(login: String, password: String): String {
  return "${URLEncoder.encode(login, StandardCharsets.UTF_8)}:${URLEncoder.encode(password, StandardCharsets.UTF_8)}"
}