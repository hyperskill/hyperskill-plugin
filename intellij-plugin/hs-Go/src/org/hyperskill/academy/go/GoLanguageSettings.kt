package org.hyperskill.academy.go

import com.goide.GoConstants.SDK_TYPE_ID
import com.goide.sdk.GoSdk
import com.goide.sdk.combobox.GoSdkChooserCombo
import com.intellij.facet.ui.ValidationResult
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil.isAncestor
import com.intellij.openapi.vfs.VirtualFileManager
import org.hyperskill.academy.go.messages.EduGoBundle
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_GO
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessage
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent

class GoLanguageSettings : LanguageSettings<GoProjectSettings>() {

  private var selectedSdk: GoSdk? = null

  override fun getSettings(): GoProjectSettings = GoProjectSettings(selectedSdk ?: GoSdk.NULL)

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val sdkChooser = GoSdkChooserCombo({ null }, { true }, { ValidationResult.OK })
    Disposer.register(disposable, sdkChooser)

    val goSdkLoadService = service<GoSdkLoadService>()
    val sdk = sdkChooser.sdk
    if (sdk != GoSdk.NULL || goSdkLoadService.isLoaded()) {
      setSdk(sdk)
    }

    sdkChooser.childComponent.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        setSdk(sdkChooser.sdk)
      }
    }

    goSdkLoadService.reloadSdk { sdkList ->
      runInEdt {
        if (disposable.isDisposed) return@runInEdt
        if (sdkList.isEmpty()) {
          setSdk(GoSdk.NULL)
        }
        else {
          sdkList.forEach { sdk ->
            sdkChooser.addSdk(sdk, true)
          }
        }
      }
    }

    return listOf(LabeledComponent.create(sdkChooser as JComponent, SDK_TYPE_ID, BorderLayout.WEST))
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    val sdk = selectedSdk ?: return SettingsValidationResult.Pending

    val sdkHomePath = VirtualFileManager.getInstance().findFileByUrl(sdk.homeUrl)?.path
    val message = when {
      sdk == GoSdk.NULL -> ValidationMessage(EduGoBundle.message("error.no.sdk", ""), ENVIRONMENT_CONFIGURATION_LINK_GO)
      !sdk.isValid -> ValidationMessage(EduGoBundle.message("error.invalid.sdk"), ENVIRONMENT_CONFIGURATION_LINK_GO)
      courseLocation != null && sdkHomePath != null && isAncestor(courseLocation, sdkHomePath, false) ->
        ValidationMessage(EduGoBundle.message("error.invalid.sdk.location"), ENVIRONMENT_CONFIGURATION_LINK_GO)

      else -> null
    }

    return SettingsValidationResult.Ready(message)
  }

  private fun setSdk(sdk: GoSdk) {
    selectedSdk = sdk
    notifyListeners()
  }
}
