package org.hyperskill.academy.learning.framework.impl

import java.io.DataOutput
import java.io.IOException

interface FrameworkStorageData {
  @Throws(IOException::class)
  fun write(out: DataOutput)
}
