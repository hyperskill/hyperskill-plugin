package org.hyperskill.academy.learning.stepik.hyperskill.settings

import com.intellij.ui.components.JBCheckBox
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.api.EduOAuthCodeFlowConnector
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.settings.OAuthLoginOptions
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillAccount
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.isHyperskillSupportAvailable
import org.hyperskill.academy.learning.stepik.hyperskill.profileUrl
import javax.swing.JComponent

class HyperskillOptions : OAuthLoginOptions<HyperskillAccount>() {
  private var automaticUpdateCheckBox: JBCheckBox = JBCheckBox(
    EduCoreBundle.message("hyperskill.settings.auto.update"),
    HyperskillSettings.INSTANCE.updateAutomatically
  )

  override val connector: EduOAuthCodeFlowConnector<HyperskillAccount, *>
    get() = HyperskillConnector.getInstance()

  override fun getDisplayName(): String = EduNames.JBA

  override fun isAvailable(): Boolean = super.isAvailable() && isHyperskillSupportAvailable()

  override fun profileUrl(account: HyperskillAccount): String = account.profileUrl

  override fun getAdditionalComponents(): List<JComponent> {
    return listOf(automaticUpdateCheckBox)
  }

  override fun apply() {
    super.apply()
    HyperskillSettings.INSTANCE.updateAutomatically = automaticUpdateCheckBox.isSelected
  }

  override fun reset() {
    super.reset()
    automaticUpdateCheckBox.isSelected = HyperskillSettings.INSTANCE.updateAutomatically
  }

  override fun isModified(): Boolean {
    return super.isModified() || HyperskillSettings.INSTANCE.updateAutomatically != automaticUpdateCheckBox.isSelected
  }
}
