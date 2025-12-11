package org.hyperskill.academy.jvm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.*
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.LocalFileSystem
import org.hyperskill.academy.jvm.messages.EduJVMBundle
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JAVA
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.project
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessage
import org.hyperskill.academy.learning.runInBackground
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent

private val LOG = logger<JdkLanguageSettings>()

open class JdkLanguageSettings : LanguageSettings<JdkProjectSettings>() {

  protected var jdk: Sdk? = null
  protected val sdkModel: ProjectSdksModel = createSdkModel()

  /**
   * Represents the state of JDK loading process.
   */
  private enum class JdkLoadingState {
    NOT_STARTED,
    LOADING,
    LOADED,
    FAILED
  }

  @Volatile
  private var loadingState: JdkLoadingState = JdkLoadingState.NOT_STARTED

  @Volatile
  private var loadingError: String? = null

  @Volatile
  private var componentsInitialized: Boolean = false

  // Keep for backward compatibility with isJdkLoading checks
  private val isJdkLoading: Boolean
    get() = loadingState == JdkLoadingState.LOADING

  private fun createSdkModel(): ProjectSdksModel {
    val project = ProjectManager.getInstance().defaultProject
    // Do NOT call reset(project) on EDT — it performs synchronous progress and is prohibited.
    // We return the configurable's model as-is and let subclasses optionally pre-populate it
    // (e.g., with a bundled JDK) via setupProjectSdksModel. Any heavier refreshes must be done
    // in background before UI selection (see preselectJdk and prewarmSdkValidation).
    return ProjectStructureConfigurable.getInstance(project).projectJdksModel.apply {
      setupProjectSdksModel(this)
    }
  }

  protected open fun setupProjectSdksModel(model: ProjectSdksModel) {}

  /**
   * Called from background thread to add bundled JDK to the model if needed.
   * Override this instead of [setupProjectSdksModel] for operations that require write actions
   * (like [ProjectSdksModel.addSdk]) which are prohibited on EDT in IntelliJ 2025.3+.
   */
  protected open fun addBundledJdkIfNeeded(model: ProjectSdksModel) {}

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    componentsInitialized = true
    val sdkTypeFilter = Condition<SdkTypeId> { sdkTypeId -> sdkTypeId is JavaSdkType && !(sdkTypeId as JavaSdkType).isDependent }
    val sdkFilter = Condition<Sdk> { sdk -> sdkTypeFilter.value(sdk.sdkType) }
    val jdkComboBox = JdkComboBox(null, sdkModel, sdkTypeFilter, sdkFilter, sdkTypeFilter, null)
    preselectJdk(course, jdkComboBox, sdkModel)
    jdk = jdkComboBox.selectedItem?.jdk
    jdkComboBox.addItemListener {
      jdk = jdkComboBox.selectedItem?.jdk
      notifyListeners()
    }

    // Subscribe to JDK table changes to update combobox when user adds/removes JDKs in Settings
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(ProjectJdkTable.JDK_TABLE_TOPIC, object : ProjectJdkTable.Listener {
      override fun jdkAdded(addedJdk: Sdk) {
        LOG.info("JDK added event received: ${addedJdk.name}, type=${addedJdk.sdkType}")
        if (addedJdk.sdkType is JavaSdkType) {
          invokeLater(ModalityState.any()) {
            LOG.info("Processing JDK added on EDT, current jdk=$jdk, loadingState=$loadingState")
            jdkComboBox.reloadModel()
            if (jdk == null) {
              jdkComboBox.selectedJdk = addedJdk
              jdk = addedJdk
              LOG.info("Selected newly added JDK: ${addedJdk.name}")
            }
            loadingState = JdkLoadingState.LOADED
            loadingError = null
            LOG.info("Updated loadingState to LOADED, calling notifyListeners")
            notifyListeners()
          }
        }
      }

      override fun jdkRemoved(removedJdk: Sdk) {
        LOG.info("JDK removed event received: ${removedJdk.name}")
        if (removedJdk.sdkType is JavaSdkType) {
          invokeLater(ModalityState.any()) {
            jdkComboBox.reloadModel()
            if (jdk == removedJdk) {
              jdk = jdkComboBox.selectedJdk
            }
            notifyListeners()
          }
        }
      }
    })

    return listOf(LabeledComponent.create(jdkComboBox, "JDK", BorderLayout.WEST))
  }

  private fun preselectJdk(course: Course, jdkComboBox: JdkComboBox, sdksModel: ProjectSdksModel) {
    if (jdkComboBox.selectedJdk != null) {
      loadingState = JdkLoadingState.LOADED
      return
    }
    loadingState = JdkLoadingState.LOADING
    loadingError = null

    // Disable combo box while loading to indicate loading state
    jdkComboBox.isEnabled = false

    runInBackground(course.project, EduJVMBundle.message("progress.setting.suitable.jdk"), false) {
      var errorOccurred = false
      var errorMessage: String? = null

      // Reset SDK model off-EDT to avoid IllegalStateException from synchronous progress on EDT
      try {
        val project = course.project
        if (project != null) {
          sdksModel.reset(project)
        }
      }
      catch (e: Throwable) {
        // best-effort; if reset fails we'll try with whatever the model currently has
        errorMessage = e.message
      }

      // Add bundled JDK if needed (must be done off-EDT as addSdk requires write action)
      try {
        addBundledJdkIfNeeded(sdksModel)
      }
      catch (_: Throwable) {
        // best-effort; ignore failures
      }

      // If sdkModel is empty, try to get JDKs directly from ProjectJdkTable
      val suitableJdk = findSuitableJdk(minJvmSdkVersion(course), sdksModel)
        ?: findSuitableJdkFromTable(minJvmSdkVersion(course))

      // Check if we found any JDK at all (either suitable or any)
      val anyJdkAvailable = sdksModel.sdks.any { it.sdkType == JavaSdk.getInstance() }
        || ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance()).isNotEmpty()

      if (!anyJdkAvailable) {
        errorOccurred = true
        errorMessage = EduJVMBundle.message("error.no.jdk.available")
      }

      // Pre-warm SDK validation and VFS lookups off the EDT to avoid slow operations during UI rendering
      prewarmSdkValidation(suitableJdk)

      invokeLater(ModalityState.any()) {
        jdkComboBox.isEnabled = true
        jdkComboBox.selectedJdk = suitableJdk
        jdk = suitableJdk

        if (errorOccurred) {
          loadingState = JdkLoadingState.FAILED
          loadingError = errorMessage
        }
        else {
          loadingState = JdkLoadingState.LOADED
          loadingError = null
        }
        notifyListeners()
      }
    }
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    LOG.info("validate called: componentsInitialized=$componentsInitialized, loadingState=$loadingState, jdk=$jdk")

    // If UI components haven't been initialized yet or JDK is still loading, return Pending to avoid false errors
    if (!componentsInitialized || loadingState == JdkLoadingState.LOADING) {
      return SettingsValidationResult.Pending
    }

    // If JDK loading previously failed but now a JDK is available, reset the error state
    if (loadingState == JdkLoadingState.FAILED) {
      // First check if user already selected a JDK in the combobox (e.g., downloaded one)
      if (jdk != null) {
        LOG.info("loadingState is FAILED but jdk is already selected: $jdk, resetting to LOADED")
        loadingState = JdkLoadingState.LOADED
        loadingError = null
      }
      else {
        // Check ProjectJdkTable as fallback (for JDKs added via Settings)
        val jdksInTable = ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance())
        LOG.info("loadingState is FAILED, jdk is null, checking ProjectJdkTable: found ${jdksInTable.size} JDKs")
        if (jdksInTable.isNotEmpty()) {
          // JDKs are now available - user added them via Settings
          loadingState = JdkLoadingState.LOADED
          loadingError = null
          jdk = findSuitableJdkFromTable(course?.let { minJvmSdkVersion(it) } ?: JavaVersionNotProvided)
          LOG.info("Auto-selected JDK from table: $jdk")
        }
        else {
          val errorMsg = loadingError ?: EduJVMBundle.message("error.jdk.loading.failed")
          LOG.info("Returning FAILED state with error: $errorMsg")
          return SettingsValidationResult.Ready(ValidationMessage(errorMsg, ENVIRONMENT_CONFIGURATION_LINK_JAVA))
        }
      }
    }

    fun ready(messageId: String, vararg additionalSubstitution: String): SettingsValidationResult {
      val message = EduJVMBundle.message(messageId, *additionalSubstitution)

      return SettingsValidationResult.Ready(ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_JAVA))
    }

    course ?: return super.validate(null, courseLocation)

    // compare the version of the selected jdk to the minimum version required by the course
    val selectedJavaVersion = ParsedJavaVersion.fromJavaSdkVersionString(jdk?.versionString)
    val courseJavaVersion = minJvmSdkVersion(course)

    if (courseJavaVersion is JavaVersionParseFailed) {
      return ready("error.unsupported.java.version", courseJavaVersion.versionAsText)
    }
    if (selectedJavaVersion is JavaVersionParseFailed) {
      return ready("failed.determine.java.version", selectedJavaVersion.versionAsText)
    }

    if (selectedJavaVersion == JavaVersionNotProvided) {
      return if (courseJavaVersion == JavaVersionNotProvided) {
        ready("error.no.jdk")
      }
      else {
        ready("error.no.jdk.need.at.least", (courseJavaVersion as JavaVersionParseSuccess).javaSdkVersion.description)
      }
    }

    if (courseJavaVersion == JavaVersionNotProvided) {
      return SettingsValidationResult.OK
    }

    selectedJavaVersion as JavaVersionParseSuccess
    courseJavaVersion as JavaVersionParseSuccess

    return if (selectedJavaVersion isAtLeast courseJavaVersion) {
      SettingsValidationResult.OK
    }
    else {
      ready("error.old.java", courseJavaVersion.javaSdkVersion.description, selectedJavaVersion.javaSdkVersion.description)
    }
  }

  /**
   * This is the minimum JDK version that we allow to use for the course.
   * Basically, it is taken from environment settings, but for Java courses it is specified explicitly in [Course.languageVersion]
   */
  protected open fun minJvmSdkVersion(course: Course): ParsedJavaVersion = course.minJvmSdkVersion

  override fun getSettings(): JdkProjectSettings = JdkProjectSettings(sdkModel, jdk)

  companion object {
    fun findBundledJdk(model: ProjectSdksModel): BundledJdkInfo? {
      val bundledJdkPath = PathManager.getBundledRuntimePath()
      // It's possible IDE doesn't have bundled jdk.
      // For example, IDE loaded by gradle-intellij-plugin doesn't have bundled jdk
      if (!File(bundledJdkPath).exists()) return null
      // Try to find existing bundled jdk added by the plugin on previous course creation or by user
      val sdk = model.projectSdks.values.find { it.homePath == bundledJdkPath }
      return BundledJdkInfo(bundledJdkPath, sdk)
    }

    fun findSuitableJdk(courseSdkVersion: ParsedJavaVersion, sdkModel: ProjectSdksModel): Sdk? {
      val jdks = sdkModel.sdks.filter { it.sdkType == JavaSdk.getInstance() }

      if (courseSdkVersion !is JavaVersionParseSuccess) {
        return jdks.firstOrNull()
      }

      return jdks.find {
        val jdkVersion = ParsedJavaVersion.fromJavaSdkVersionString(it.versionString)
        if (jdkVersion is JavaVersionParseSuccess) {
          jdkVersion isAtLeast courseSdkVersion
        }
        else {
          false
        }
      }
    }

    /**
     * Fallback method to find suitable JDK directly from ProjectJdkTable
     * when ProjectSdksModel is empty (e.g., when course.project is null in Browse Courses dialog).
     */
    fun findSuitableJdkFromTable(courseSdkVersion: ParsedJavaVersion): Sdk? {
      val jdks = ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance())

      if (courseSdkVersion !is JavaVersionParseSuccess) {
        return jdks.firstOrNull()
      }

      return jdks.find {
        val jdkVersion = ParsedJavaVersion.fromJavaSdkVersionString(it.versionString)
        if (jdkVersion is JavaVersionParseSuccess) {
          jdkVersion isAtLeast courseSdkVersion
        }
        else {
          false
        }
      }
    }
  }

  data class BundledJdkInfo(val path: String, val existingSdk: Sdk?)

  /**
   * Perform potentially slow checks and VFS access off the EDT so that UI selection/painting
   * of the JDK combo box does not trigger SlowOperations violations on the UI thread.
   */
  private fun prewarmSdkValidation(sdk: Sdk?) {
    if (sdk == null) return
    val homePath = sdk.homePath ?: return
    // Touch VFS for the SDK home directory in BGT
    try {
      val lfs = LocalFileSystem.getInstance()
      lfs.refreshAndFindFileByPath(homePath)
    }
    catch (_: Throwable) {
      // best-effort pre-warm; ignore failures
    }

    // Trigger common queries that are used by renderers off-EDT to populate caches if any
    try {
      // Access version string (may compute using filesystem)
      @Suppress("UNUSED_VARIABLE")
      val ignoredVersionString = sdk.versionString
      // Access VirtualFile home directory through concrete impl to trigger internal resolution
      if (sdk is ProjectJdkImpl) {
        @Suppress("UNUSED_VARIABLE")
        val ignoredHomeDirectory = sdk.homeDirectory
      }
      // Validate SDK path using SdkType logic off-EDT (renderers call this on EDT)
      val sdkTypeId: SdkTypeId = sdk.sdkType
      if (sdkTypeId is SdkType) {
        @Suppress("UNUSED_VARIABLE")
        val ignoredHasValidPath = sdkTypeId.sdkHasValidPath(sdk)
      }
    }
    catch (_: Throwable) {
      // best-effort pre-warm; ignore failures
    }
  }
}
