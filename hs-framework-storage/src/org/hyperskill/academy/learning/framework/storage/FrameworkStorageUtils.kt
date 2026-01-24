package org.hyperskill.academy.learning.framework.storage

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

object FrameworkStorageUtils {
  @Throws(IOException::class)
  fun writeINT(out: DataOutput, value: Int) {
    if (value >= 0 && value < 192) {
      out.writeByte(value)
    } else {
      out.writeByte(192 + (value and 0x3f))
      writeINT(out, value ushr 6)
    }
  }

  @Throws(IOException::class)
  fun readINT(input: DataInput): Int {
    val b = input.readUnsignedByte()
    return if (b < 192) {
      b
    } else {
      (b - 192) + (readINT(input) shl 6)
    }
  }

  @Throws(IOException::class)
  fun writeLONG(out: DataOutput, value: Long) {
    if (value >= 0 && value < 192) {
      out.writeByte(value.toInt())
    } else {
      out.writeByte(192 + (value.toInt() and 0x3f))
      writeLONG(out, value ushr 6)
    }
  }

  @Throws(IOException::class)
  fun readLONG(input: DataInput): Long {
    val b = input.readUnsignedByte()
    return if (b < 192) {
      b.toLong()
    } else {
      (b - 192).toLong() + (readLONG(input) shl 6)
    }
  }
}
