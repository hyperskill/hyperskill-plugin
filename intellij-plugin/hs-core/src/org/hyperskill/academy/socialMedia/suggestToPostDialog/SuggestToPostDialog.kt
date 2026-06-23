package org.hyperskill.academy.socialMedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DoNotAskOption
import org.hyperskill.academy.learning.EduBrowser
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.socialMedia.SocialMediaSettings
import org.hyperskill.academy.socialMedia.SocialMediaUtils
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JComponent

/**
 * Modal dialog suggesting to share the Hyperskill project achievement.
 * Provides "Share on X" and "Share on LinkedIn" buttons that open the corresponding web share intents,
 * a "Close" button, and a "Don't ask again" checkbox that disables the suggestion via [SocialMediaSettings].
 */
class SuggestToPostDialog(
  project: Project,
  message: String,
  private val xShareUrl: String,
  private val linkedInShareUrl: String
) : SuggestToPostDialogUI, DialogWrapper(project) {

  private val panel = SuggestToPostDialogPanel(message, SocialMediaUtils.loadAchievementImage())

  init {
    title = EduCoreBundle.message("social.media.suggest.to.post.dialog.title")
    setCancelButtonText(EduCoreBundle.message("social.media.close.button.text"))
    isResizable = false
    installDoNotAskOption()
    init()
  }

  // `setDoNotAskOption` is the standard way to add a "do not ask again" checkbox to a custom `DialogWrapper`.
  // It is deprecated in the platform but kept intentionally as it works across all supported versions.
  @Suppress("DEPRECATION")
  private fun installDoNotAskOption() {
    setDoNotAskOption(DoNotAskToSuggestOption())
  }

  override fun createCenterPanel(): JComponent = panel

  // Share buttons open a browser but keep the dialog open so the user can share to both networks
  override fun createActions(): Array<Action> = arrayOf(
    ShareAction(EduCoreBundle.message("social.media.share.on.x.button.text"), xShareUrl),
    ShareAction(EduCoreBundle.message("social.media.share.on.linkedin.button.text"), linkedInShareUrl),
    cancelAction
  )

  private class ShareAction(name: String, private val url: String) : AbstractAction(name) {
    override fun actionPerformed(e: ActionEvent?) {
      EduBrowser.getInstance().browse(url)
    }
  }

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
