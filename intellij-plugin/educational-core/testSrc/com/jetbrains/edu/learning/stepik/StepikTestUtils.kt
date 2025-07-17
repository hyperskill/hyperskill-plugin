package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.authUtils.TokenInfo
import java.text.SimpleDateFormat
import java.util.*

object StepikTestUtils {

  fun loginFakeStepikUser() {
    val fakeToken = TokenInfo().apply { accessToken = "faketoken" }
    EduSettings.getInstance().user = StepikUser.createEmptyUser().apply {
      userInfo = StepikUserInfo("Test User")
      userInfo.id = 1
      saveTokens(fakeToken)
    }
  }

  fun logOutFakeStepikUser() {
    EduSettings.getInstance().user = null
  }

  fun Date.format(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    return formatter.format(this)
  }
}
