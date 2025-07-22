package org.hyperskill.academy.learning.stepik.hyperskill.widget

import com.intellij.openapi.project.Project
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.LoginWidget
import org.hyperskill.academy.learning.api.EduOAuthCodeFlowConnector
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillAccount
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.profileUrl
import org.hyperskill.academy.learning.stepik.hyperskill.update.SyncHyperskillCourseAction

class HyperskillWidget(project: Project) : LoginWidget<HyperskillAccount>(
  project,
  EduCoreBundle.message("hyperskill.widget.title"),
  EduCoreBundle.message("hyperskill.widget.tooltip"),
  EducationalCoreIcons.Platform.JetBrainsAcademy
) {
  override val connector: EduOAuthCodeFlowConnector<HyperskillAccount, *>
    get() = HyperskillConnector.getInstance()

  override val synchronizeCourseActionId: String
    get() = SyncHyperskillCourseAction.ACTION_ID

  override fun profileUrl(account: HyperskillAccount): String = account.profileUrl

  override fun ID() = "HyperskillAccountWidget"

}
