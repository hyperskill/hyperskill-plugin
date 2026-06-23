package org.hyperskill.academy.socialMedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DoNotAskOption
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.socialMedia.SocialMediaSettings
import org.hyperskill.academy.socialMedia.SocialMediaUtils
import javax.swing.JComponent

/**
 * Modal dialog suggesting to share the Hyperskill project achievement.
 * The "OK" button is labeled "Learn more" and the caller opens [SocialMediaUtils.LEARN_MORE_URL] when it is pressed.
 * A "Don't ask again" checkbox disables the suggestion via [SocialMediaSettings].
 */
class SuggestToPostDialog(project: Project, message: String) : SuggestToPostDialogUI, DialogWrapper(project) {

  private val panel = SuggestToPostDialogPanel(message, SocialMediaUtils.loadAchievementImage())

  init {
    title = EduCoreBundle.message("social.media.suggest.to.post.dialog.title")
    setOKButtonText(EduCoreBundle.message("social.media.learn.more.button.text"))
    setCancelButtonText(EduCoreBundle.message("social.media.close.button.text"))
    isResizable = false
    setDoNotAskOption(DoNotAskToSuggestOption())
    init()
  }

  override fun createCenterPanel(): JComponent = panel

  private class DoNotAskToSuggestOption : DoNotAskOption.Adapter() {
    override fun rememberChoice(isSelected: Boolean, exitCode: Int) {
      // `isSelected` means "keep showing the dialog next time"; an unchecked box means the user opted out
      if (!isSelected) {
        SocialMediaSettings.getInstance().askToPost = false
      }
    }

    // Persist the opt-out even if the dialog is closed via the "Close" (cancel) button
    override fun shouldSaveOptionsOnCancel(): Boolean = true

    override fun getDoNotShowMessage(): String = EduCoreBundle.message("social.media.do.not.ask.dialog.checkbox")
  }
}
