package org.hyperskill.academy.socialMedia

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.settings.OptionsProvider
import javax.swing.JComponent

/**
 * Settings checkbox that lets the user re-enable the "share your achievement" suggestion
 * after it was turned off via the "Don't ask again" checkbox.
 */
class SocialMediaOptionsProvider : OptionsProvider {

  private val askToPostCheckBox = JBCheckBox(EduCoreBundle.message("social.media.settings.prompt.to.share"))

  override fun createComponent(): JComponent = panel {
    row {
      cell(askToPostCheckBox)
    }
  }

  override fun isModified(): Boolean = askToPostCheckBox.isSelected != SocialMediaSettings.getInstance().askToPost

  override fun apply() {
    SocialMediaSettings.getInstance().askToPost = askToPostCheckBox.isSelected
  }

  override fun reset() {
    askToPostCheckBox.isSelected = SocialMediaSettings.getInstance().askToPost
  }

  override fun getDisplayName(): String = EduCoreBundle.message("social.media.settings.display.name")
}
