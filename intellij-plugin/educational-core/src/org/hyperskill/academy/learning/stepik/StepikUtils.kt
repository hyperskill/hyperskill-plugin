/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:JvmName("StepikUtils")

package org.hyperskill.academy.learning.stepik

import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager

@Suppress("DialogTitleCapitalization")
fun showUpdateAvailableNotification(project: Project, updateAction: () -> Unit) {
  EduNotificationManager
    .create(INFORMATION, EduCoreBundle.message("update.content"), EduCoreBundle.message("update.content.request"))
    .setListener { notification, _ ->
      FileEditorManagerEx.getInstanceEx(project).closeAllFiles()
      notification.expire()
      ProgressManager.getInstance().runProcessWithProgressSynchronously(
        {
          ProgressManager.getInstance().progressIndicator.isIndeterminate = true
          updateAction()
        },
        EduCoreBundle.message("push.course.updating.progress.text"), true, project
      )
    }
    .notify(project)
}
