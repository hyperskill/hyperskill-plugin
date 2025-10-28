package org.hyperskill.academy.ai.debugger.core.service

import com.jetbrains.educational.ml.debugger.dto.TaskDescriptionFormat
import com.jetbrains.educational.ml.debugger.request.TaskDescriptionBase

data class TaskDescription(
  override val text: String,
  override val descriptionFormat: TaskDescriptionFormat
) : TaskDescriptionBase()