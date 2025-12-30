package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.*
import com.jetbrains.python.sdk.add.PySdkPathChoosingComboBox
import com.jetbrains.python.sdk.add.addInterpretersAsync
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessage
import org.hyperskill.academy.learning.newproject.ui.errors.ready
import org.hyperskill.academy.python.learning.messages.EduPythonBundle
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.event.ItemEvent
import javax.swing.JComponent

@Suppress("DEPRECATION") // PySdkPathChoosingComboBox and addInterpretersAsync are deprecated but no clear replacement is available yet
open class PyLanguageSettings : LanguageSettings<PyProjectSettings>() {

  private val projectSettings: PyProjectSettings = PyProjectSettings()
  private var isSettingsInitialized = false

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {

    val sdkField = PySdkPathChoosingComboBox()
    sdkField.childComponent.addItemListener {
      if (it.stateChange == ItemEvent.SELECTED) {
        projectSettings.sdk = sdkField.selectedSdk
        notifyListeners()
      }
    }

    addInterpretersAsync(sdkField, {
      collectPySdks(course, context ?: UserDataHolderBase())
    }) {
      if (disposable.isDisposed) return@addInterpretersAsync
      projectSettings.sdk = sdkField.selectedSdk
      isSettingsInitialized = true
      notifyListeners()
    }

    return listOf<LabeledComponent<JComponent>>(
      LabeledComponent.create(sdkField, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST)
    )
  }

  // Inspired by `com.jetbrains.python.sdk.add.PyAddSdkPanelKt.addBaseInterpretersAsync` implementation
  @RequiresBackgroundThread
  private fun collectPySdks(course: Course, context: UserDataHolder): List<Sdk> {
    // Find all base Python SDKs
    val baseSdks = findBaseSdks(emptyList(), null, context)
      // It's important to check validity here, in background thread,
      // because it caches a result of checking if python binary is executable.
      // If the first (uncached) invocation is invoked in EDT, it may throw exception and break UI rendering.
      // See https://youtrack.jetbrains.com/issue/EDU-6371
      .filter { it.sdkSeemsValid }

    if (baseSdks.isEmpty()) {
      return getSdksToInstall()
    }

    // Create fake SDK (PySdkToCreateVirtualEnv) for each base SDK
    // This ensures all SDKs use the same code path and work consistently
    val fakeSdks = baseSdks.mapNotNull { baseSdk ->
      val homePath = baseSdk.homePath ?: return@mapNotNull null
      val languageLevel = baseSdk.languageLevel

      // Filter out SDKs that don't match course requirements
      if (isSdkApplicable(course, languageLevel) is Err) {
        LOG.warn("PyLanguageSettings: Skipping SDK with languageLevel=$languageLevel (doesn't match course requirements)")
        return@mapNotNull null
      }

      val pythonVersion = languageLevel.toPythonVersion()

      // Create display name like "Python 3.13"
      val name = "Python ${languageLevel.majorVersion}.${languageLevel.minorVersion}"

      LOG.warn("PyLanguageSettings: Creating fake SDK with name='$name', homePath='$homePath', version='$pythonVersion'")

      PySdkToCreateVirtualEnv.create(name, homePath, pythonVersion)
    }
      // Sort by version descending - newer versions first
      .sortedByDescending { it.languageLevel }

    return fakeSdks.takeIf { it.isNotEmpty() } ?: getSdksToInstall()
  }

  override fun getSettings(): PyProjectSettings = projectSettings

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    course ?: return SettingsValidationResult.OK
    LOG.warn("validate: course=${course.name}, courseLocation=$courseLocation, isSettingsInitialized=$isSettingsInitialized")

    // Check if this is an existing project by looking for SDK in jdk.table.xml
    // We only check in-memory SDK registry, not filesystem, to avoid slow operations on EDT
    val existingProjectSdk = courseLocation?.let { location ->
      val venvPath = "$location/.idea/VirtualEnvironment"
      LOG.warn("validate: Looking for SDK with venvPath=$venvPath")

      val allSdks = com.jetbrains.python.configuration.PyConfigurableInterpreterList.getInstance(null).allPythonSdks
      LOG.warn("validate: Found ${allSdks.size} SDKs in jdk.table.xml")

      allSdks.find { sdk ->
        val sdkPath = sdk.homePath
        LOG.warn("validate: Checking SDK ${sdk.name} at $sdkPath")
        sdkPath != null && sdkPath.startsWith(venvPath)
      }?.also {
        LOG.warn("validate: Found existing SDK for this project: ${it.name} at ${it.homePath}")
      }
    }

    val sdk = existingProjectSdk ?: projectSettings.sdk ?: return if (isSettingsInitialized) {
      LOG.warn("validate: SDK is null, isSettingsInitialized=$isSettingsInitialized")
      ValidationMessage(
        EduPythonBundle.message("error.no.python.interpreter", ENVIRONMENT_CONFIGURATION_LINK_PYTHON),
        ENVIRONMENT_CONFIGURATION_LINK_PYTHON
      ).ready()
    }
    else {
      LOG.warn("validate: SDK is null, pending")
      SettingsValidationResult.Pending
    }

    val languageLevel = sdk.languageLevel
    LOG.warn("validate: SDK name=${sdk.name}, type=${sdk.javaClass.simpleName}, languageLevel=$languageLevel, homePath=${sdk.homePath}")
    val sdkApplicable = isSdkApplicable(course, languageLevel)
    LOG.warn("validate: isSdkApplicable result = $sdkApplicable for course languageVersion=${course.languageVersion}")
    if (sdkApplicable is Err) {
      val message = "${sdkApplicable.error}<br>${EduPythonBundle.message("configure.python.environment.help")}"
      val validationMessage = ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_PYTHON)
      return SettingsValidationResult.Ready(validationMessage)
    }

    return SettingsValidationResult.OK
  }

  private val Sdk.languageLevel: LanguageLevel
    get() {
      return when (this) {
        is PySdkToCreateVirtualEnv -> {
          val pythonVersion = versionString
          if (pythonVersion == null) {
            LanguageLevel.getDefault()
          }
          else {
            LanguageLevel.fromPythonVersion(pythonVersion) ?: LanguageLevel.getDefault()
          }
        }

        is PyDetectedSdk -> {
          // PyDetectedSdk has empty `sdk.versionString`, so we should manually get language level from homePath if it exists
          homePath?.let {
            sdkFlavor.getLanguageLevel(it)
          } ?: LanguageLevel.getDefault()
        }

        else -> {
          val flavor = PythonSdkFlavor.getFlavor(this)
          homePath?.let {
            flavor?.getLanguageLevel(it)
          } ?: LanguageLevel.getDefault()
        }
      }
    }

  companion object {
    private val LOG = logger<PyLanguageSettings>()

    private val OK = Ok(Unit)

    private fun isSdkApplicable(course: Course, sdkLanguageLevel: LanguageLevel): Result<Unit, String> {
      val courseLanguageVersion = course.languageVersion
      val isPython2Sdk = sdkLanguageLevel.isPython2

      return when (courseLanguageVersion) {
        null, ALL_VERSIONS -> OK
        PYTHON_2_VERSION -> if (isPython2Sdk) OK else NoApplicablePythonError(2)
        PYTHON_3_VERSION -> if (!isPython2Sdk) OK else NoApplicablePythonError(3)
        else -> {
          val courseLanguageLevel = LanguageLevel.fromPythonVersion(courseLanguageVersion)
          when {
            courseLanguageLevel?.isPython2 != isPython2Sdk -> SpecificPythonRequiredError(courseLanguageVersion)
            sdkLanguageLevel.isAtLeast(courseLanguageLevel) -> OK
            else -> SpecificPythonRequiredError(courseLanguageVersion)
          }
        }
      }
    }

    @RequiresBackgroundThread
    private fun getBaseSdk(course: Course, context: UserDataHolder? = null): PyBaseSdkDescriptor? {
      val baseSdks = PyBaseSdksProvider.getBaseSdks(context)
      if (baseSdks.isEmpty()) {
        return null
      }
      return baseSdks.filter { isSdkApplicable(course, it.languageLevel) == OK }.maxByOrNull { it.languageLevel }
    }

    private class NoApplicablePythonError(
      requiredVersion: Int,
      errorMessage: @Nls String = EduPythonBundle.message(
        "error.incorrect.python",
        requiredVersion
      )
    ) : Err<String>(errorMessage)

    private class SpecificPythonRequiredError(
      requiredVersion: String,
      errorMessage: @Nls String = EduPythonBundle.message(
        "error.old.python",
        requiredVersion
      )
    ) : Err<String>(
      errorMessage
    )

    @RequiresBackgroundThread
    private fun createFakeSdk(course: Course, context: UserDataHolder): Sdk? {
      val baseSdk = getBaseSdk(course, context) ?: return null
      val flavor = PythonSdkFlavor.getApplicableFlavors(false)[0]
      val prefix = flavor.name + " "
      val version = baseSdk.version
      if (prefix !in version) {
        return null
      }
      val pythonVersion = version.substring(prefix.length)
      val name = "new virtual env $pythonVersion"

      return PySdkToCreateVirtualEnv.create(name, baseSdk.path, pythonVersion)
    }

    const val ALL_VERSIONS = "All versions"
  }
}
