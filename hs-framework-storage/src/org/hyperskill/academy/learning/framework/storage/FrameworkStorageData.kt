package org.hyperskill.academy.learning.framework.storage

import java.io.DataOutput
import java.io.IOException

interface FrameworkStorageData {
  @Throws(IOException::class)
  fun write(out: DataOutput)
}
