package org.hyperskill.academy.remote

import com.intellij.openapi.project.NOTIFICATIONS_SILENT_MODE
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.disableTryUltimate
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.RemoteEnvHelper.Companion.isRemoteDevServer

class EduRemoteStartupActivity : StartupActivity.DumbAware {
  override fun runActivity(project: Project) {
    if (!project.isEduProject() || !isRemoteDevServer()) return
    NOTIFICATIONS_SILENT_MODE.set(project, true)
    // Disables editor notifications which promote IDE/plugins.
    // They don't make sense in remote environment since users don't control environment in this case
    disableTryUltimate(project)
  }
}
