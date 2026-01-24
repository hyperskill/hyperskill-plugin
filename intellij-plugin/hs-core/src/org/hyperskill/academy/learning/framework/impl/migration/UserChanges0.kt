package org.hyperskill.academy.learning.framework.impl.migration

import com.intellij.util.io.DataInputOutputUtil
import org.hyperskill.academy.learning.framework.storage.Change
import org.hyperskill.academy.learning.framework.storage.FrameworkStorageData
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

data class UserChanges0(val changes: List<Change>) : FrameworkStorageData {

  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { Change.writeChange(it, out) }
  }

  companion object {

    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges0 {
      val size = DataInputOutputUtil.readINT(input)
      val changes = ArrayList<Change>(size)
      for (i in 0 until size) {
        changes += Change.readChange(input)
      }
      return UserChanges0(changes)
    }
  }
}
