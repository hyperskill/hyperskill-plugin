package org.hyperskill.academy.learning.stepik

import java.text.SimpleDateFormat
import java.util.*

object StepikTestUtils {

  fun Date.format(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
    return formatter.format(this)
  }
}
