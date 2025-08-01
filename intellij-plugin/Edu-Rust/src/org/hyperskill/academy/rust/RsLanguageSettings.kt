package org.hyperskill.academy.rust

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_RUST
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.checkIsBackgroundThread
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessage
import org.hyperskill.academy.rust.messages.EduRustBundle
import org.rust.cargo.project.RsToolchainPathChoosingComboBox
import org.rust.cargo.toolchain.RsToolchainBase
import org.rust.cargo.toolchain.RsToolchainProvider
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor
import java.awt.BorderLayout
import java.nio.file.Path
import javax.swing.JComponent

class RsLanguageSettings : LanguageSettings<RsProjectSettings>() {

  private var toolchainComboBox: RsToolchainPathChoosingComboBox? = null

  private var loadingFinished: Boolean = false

  private var rustToolchain: RsToolchainBase? = null

  override fun getSettings(): RsProjectSettings = RsProjectSettings(rustToolchain)

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val comboBox = RsToolchainPathChoosingComboBox { updateToolchain() }
    Disposer.register(disposable, comboBox)
    comboBox.addToolchainsAsync(::findAllToolchainsPath) {
      loadingFinished = true
      if (disposable.isDisposed) return@addToolchainsAsync
      // `RsToolchainPathChoosingComboBox` sets initial empty text after addition of all items
      // But we want to show text of selected item
      val combobox = comboBox.childComponent
      val selectedItem = combobox.selectedItem
      if (selectedItem is Path) {
        comboBox.selectedPath = selectedItem
      }
      updateToolchain()
    }

    toolchainComboBox = comboBox

    return listOf(
      LabeledComponent.create(
        comboBox,
        EduRustBundle.message("toolchain.label.text"),
        BorderLayout.WEST
      )
    )
  }

  private fun findAllToolchainsPath(): List<Path> {
    checkIsBackgroundThread()
    return RsToolchainFlavor.getApplicableFlavors().flatMap { it.suggestHomePaths() }.distinct()
  }

  private fun updateToolchain() {
    // Unfortunately, `RsToolchainPathChoosingComboBox` changes its text before final callback is called
    // To avoid unexpected updates of toolchain, just skip all changes before call of final callback
    if (!loadingFinished) return
    val toolchainPath = toolchainComboBox?.selectedPath
    // We already have toolchain for this path
    if (rustToolchain?.location == toolchainPath) return

    rustToolchain = toolchainPath?.let { RsToolchainProvider.getToolchain(it) }

    notifyListeners()
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    if (!loadingFinished) return SettingsValidationResult.Pending
    val toolchain = rustToolchain
    val validationMessage = when {
      toolchain == null -> {
        ValidationMessage(EduRustBundle.message("error.no.toolchain.location", ""), ENVIRONMENT_CONFIGURATION_LINK_RUST)
      }

      !toolchain.looksLikeValidToolchain() -> {
        ValidationMessage(EduRustBundle.message("error.incorrect.toolchain.location"), ENVIRONMENT_CONFIGURATION_LINK_RUST)
      }

      else -> null
    }
    return SettingsValidationResult.Ready(validationMessage)
  }

  private fun RsToolchainPathChoosingComboBox.addToolchainsAsync(
    toolchainObtainer: () -> List<Path>,
    onFinish: () -> Unit
  ) {
    setBusy(true)
    ApplicationManager.getApplication().executeOnPooledThread {
      val toolchainPaths = try {
        toolchainObtainer()
      }
      catch (e: Throwable) {
        LOG.error(e)
        emptyList()
      }
      // `RsToolchainPathChoosingComboBox` is shown inside dialog,
      // so without proper modality state `invokeLater` won't be process until dialog closed
      invokeLater(ModalityState.any()) {
        setToolchains(toolchainPaths)
        setBusy(false)
        onFinish()
      }
    }
  }

  companion object {
    private val LOG = logger<RsLanguageSettings>()
  }
}
