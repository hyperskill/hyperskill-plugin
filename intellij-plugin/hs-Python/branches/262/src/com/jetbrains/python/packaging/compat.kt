package com.jetbrains.python.packaging

import com.intellij.openapi.util.NlsContexts
import com.jetbrains.python.errorProcessing.Exe
import com.jetbrains.python.errorProcessing.ExecError
import com.jetbrains.python.errorProcessing.ExecErrorImpl
import com.jetbrains.python.errorProcessing.ExecErrorReason
import java.io.IOException

fun createForTimeout(
  additionalMessageToUser: @NlsContexts.DialogMessage String?,
  command: String,
  args: List<String>,
): ExecError = ExecErrorImpl(
  exe = Exe.fromString(command),
  args = args.toTypedArray(),
  errorReason = ExecErrorReason.Timeout,
  additionalMessageToUser = additionalMessageToUser,
)

fun createForException(
  exception: IOException,
  additionalMessageToUser: @NlsContexts.DialogMessage String?,
  command: String,
  args: List<String>,
): ExecError = ExecErrorImpl(
  exe = Exe.fromString(command),
  args = args.toTypedArray(),
  errorReason = ExecErrorReason.CantStart(null, exception.localizedMessage),
  additionalMessageToUser = additionalMessageToUser,
)