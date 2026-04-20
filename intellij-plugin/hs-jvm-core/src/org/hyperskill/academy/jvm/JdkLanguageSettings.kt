package org.hyperskill.academy.jvm

import com.intellij.openapi.application.*
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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.LocalFileSystem
import kotlinx.coroutines.*
import org.hyperskill.academy.jvm.messages.EduJVMBundle
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JAVA
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.project
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessage
import java.awt.BorderLayout
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
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

  private val preselectRequestId = AtomicInteger()

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
    val jdkComboBox = JdkComboBox(course.project, sdkModel, sdkTypeFilter, sdkFilter, sdkTypeFilter, null)
    val uiScope = context?.getUserData(COROUTINE_SCOPE_KEY) ?: createFallbackUiScope(disposable)
    preselectJdk(course, jdkComboBox, sdkModel, uiScope)
    jdk = jdkComboBox.selectedItem?.jdk
    jdkComboBox.addItemListener {
      updateSelectedJdk(jdkComboBox.selectedItem?.jdk)
    }

    // Subscribe to JDK table changes to update combobox when user adds/removes JDKs in Settings
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(ProjectJdkTable.JDK_TABLE_TOPIC, object : ProjectJdkTable.Listener {
      override fun jdkAdded(addedJdk: Sdk) {
        LOG.info("JDK added event received: ${addedJdk.name}, type=${addedJdk.sdkType}")
        if (addedJdk.sdkType is JavaSdkType) {
          invalidatePreselectRequests()
          invokeLater(ModalityState.any()) {
            if (!canUpdateJdkUi(jdkComboBox, course.project)) return@invokeLater
            LOG.info("Processing JDK added on EDT, current jdk=$jdk, loadingState=$loadingState")
            jdkComboBox.reloadModel()
            jdkComboBox.isEnabled = true
            if (jdk == null || loadingState == JdkLoadingState.FAILED) {
              jdkComboBox.selectedJdk = addedJdk
              LOG.info("Selected newly added JDK: ${addedJdk.name}")
            }
            updateSelectedJdk(jdkComboBox.selectedJdk ?: addedJdk)
            LOG.info("Updated loadingState to LOADED, calling notifyListeners")
          }
        }
      }

      override fun jdkRemoved(removedJdk: Sdk) {
        LOG.info("JDK removed event received: ${removedJdk.name}")
        if (removedJdk.sdkType is JavaSdkType) {
          invalidatePreselectRequests()
          invokeLater(ModalityState.any()) {
            if (!canUpdateJdkUi(jdkComboBox, course.project)) return@invokeLater
            jdkComboBox.reloadModel()
            jdkComboBox.isEnabled = true
            val selectedJdk = if (jdk == removedJdk) jdkComboBox.selectedJdk else jdk
            if (selectedJdk != null) {
              updateSelectedJdk(selectedJdk)
            }
            else if (!hasAnyJdks(sdkModel)) {
              loadingState = JdkLoadingState.FAILED
              loadingError = EduJVMBundle.message("error.no.jdk.available", ENVIRONMENT_CONFIGURATION_LINK_JAVA)
              notifyListeners()
            }
          }
        }
      }
    })

    return listOf(LabeledComponent.create(jdkComboBox, "JDK", BorderLayout.WEST))
  }

  private fun preselectJdk(
    course: Course,
    jdkComboBox: JdkComboBox,
    sdksModel: ProjectSdksModel,
    uiScope: CoroutineScope
  ) {
    if (jdkComboBox.selectedJdk != null) {
      loadingState = JdkLoadingState.LOADED
      return
    }
    val requestId = preselectRequestId.incrementAndGet()
    loadingState = JdkLoadingState.LOADING
    loadingError = null

    // Disable combo box while loading to indicate loading state
    LOG.info("Starting JDK preselection request #$requestId")
    jdkComboBox.isEnabled = false

    uiScope.launch {
      LOG.info("Running JDK preselection request #$requestId in background")

      val result =
        withContext(Dispatchers.IO) {
          try {
            val project = readAction { course.project }
            LOG.info("Resetting SDK model for request #$requestId")
            if (project != null) {
              sdksModel.reset(project)
            }
            LOG.info("SDK model reset finished for request #$requestId")

            LOG.info("add bundled jdk if needed for request #$requestId")
            addBundledJdkIfNeeded(sdksModel)

            LOG.info("Sync sdks model with jdk table for request #$requestId")
            syncSdksModelWithJdkTable(sdksModel)

            LOG.info("Find sdks for request #$requestId")
            val suitableJdk = findSuitableJdk(minJvmSdkVersion(course), sdksModel)
                              ?: findSuitableJdkFromTable(minJvmSdkVersion(course))
            val hasAnyJdks = hasAnyJdks(sdksModel)

            suitableJdk?.let {
              prewarmSdkValidation(it)
            }

            PreselectJdkResult(
              suitableJdk = suitableJdk,
              loadingState = if (hasAnyJdks) JdkLoadingState.LOADED else JdkLoadingState.FAILED,
              loadingError = if (hasAnyJdks) null else EduJVMBundle.message("error.no.jdk.available", ENVIRONMENT_CONFIGURATION_LINK_JAVA)
            )
          }
          catch (e: CancellationException) {
            LOG.error("Failed to preselect JDK for request #$requestId", e)
            throw e
          }
          catch (e: Throwable) {
            LOG.warn("Failed to preselect JDK for request #$requestId", e)
            PreselectJdkResult.failed(e.message ?: EduJVMBundle.message("error.jdk.loading.failed", ENVIRONMENT_CONFIGURATION_LINK_JAVA))
          }
        }

      LOG.info(
        "JDK preselection request #$requestId finished: state=${result.loadingState}, " +
        "suitableJdk=${result.suitableJdk?.name}, error=${result.loadingError}"
      )

      withContext(Dispatchers.EDT) {
        LOG.info("Finishing JDK preselection request #$requestId on EDT")
        if (!canApplyPreselectResult(requestId, jdkComboBox, course.project)) {
          LOG.info(
            "comboDisplayable=${jdkComboBox.isDisplayable}, comboShowing=${jdkComboBox.isShowing}, " +
            "projectDisposed=${course.project?.isDisposed == true}, latestRequest=${preselectRequestId.get()}"
          )
          return@withContext
        }
        finishPreselectJdk(jdkComboBox, result)
      }
    }
  }

  private fun updateSelectedJdk(selectedJdk: Sdk?) {
    jdk = selectedJdk
    if (selectedJdk != null) {
      updateLoadingState(true)
    }
    notifyListeners()
  }

  private fun updateLoadingState(hasAnyJdks: Boolean, errorMessage: String? = null) {
    loadingState = if (hasAnyJdks) JdkLoadingState.LOADED else JdkLoadingState.FAILED
    loadingError = if (hasAnyJdks) null else errorMessage ?: EduJVMBundle.message("error.jdk.loading.failed", ENVIRONMENT_CONFIGURATION_LINK_JAVA)
  }

  private fun finishPreselectJdk(jdkComboBox: JdkComboBox, result: PreselectJdkResult) {
    LOG.info("JDK preselection finished: state=${result.loadingState}, suitableJdk=${result.suitableJdk?.name}, error=${result.loadingError}")
    loadingState = result.loadingState
    loadingError = result.loadingError
    jdkComboBox.reloadModel()
    jdkComboBox.isEnabled = true
    val jdkToSelect = jdk ?: jdkComboBox.selectedJdk ?: result.suitableJdk
    if (jdkComboBox.selectedJdk != jdkToSelect) {
      jdkComboBox.selectedJdk = jdkToSelect
    }
    updateSelectedJdk(jdkComboBox.selectedJdk ?: jdkToSelect)
  }

  private fun canApplyPreselectResult(
    requestId: Int,
    jdkComboBox: JdkComboBox,
    project: com.intellij.openapi.project.Project?
  ): Boolean {
    return requestId == preselectRequestId.get()
           && canUpdateJdkUi(jdkComboBox, project)
  }

  private fun canUpdateJdkUi(
    jdkComboBox: JdkComboBox,
    project: com.intellij.openapi.project.Project?,
  ): Boolean {
    if (project?.isDisposed == true) {
      return false
    }
    return jdkComboBox.isDisplayable || jdkComboBox.parent != null
  }

  private fun invalidatePreselectRequests() {
    preselectRequestId.incrementAndGet()
  }

  private fun createFallbackUiScope(disposable: CheckedDisposable): CoroutineScope {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.EDT + ModalityState.any().asContextElement())
    Disposer.register(disposable) {
      scope.cancel()
    }
    return scope
  }

  private fun hasAnyJdks(sdksModel: ProjectSdksModel): Boolean {
    return sdksModel.sdks.any { it.sdkType is JavaSdkType }
           || ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance()).isNotEmpty()
  }

  private fun syncSdksModelWithJdkTable(sdksModel: ProjectSdksModel) {
    val modelJdks = sdksModel.sdks
      .filter { it.sdkType is JavaSdkType }
      .map { it.name to it.homePath }
      .toSet()

    ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance()).forEach { sdk ->
      val sdkKey = sdk.name to sdk.homePath
      if (sdkKey !in modelJdks) {
        LOG.info("Adding table JDK to SDK model: ${sdk.name}")
        sdksModel.addSdk(sdk)
      }
    }
  }

  private data class PreselectJdkResult(
    val suitableJdk: Sdk?,
    val loadingState: JdkLoadingState,
    val loadingError: String?
  ) {
    companion object {
      fun failed(message: String): PreselectJdkResult =
        PreselectJdkResult(
          suitableJdk = null,
          loadingState = JdkLoadingState.FAILED,
          loadingError = message
        )
    }
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    LOG.info("validate called: componentsInitialized=$componentsInitialized, loadingState=$loadingState, jdk=$jdk")

    // If UI components haven't been initialized yet or JDK is still loading, return Pending to avoid false errors
    if (!componentsInitialized || loadingState == JdkLoadingState.LOADING) {
      LOG.info("componentsInitialized is false or loadingState is LOADING, returning Pending: $loadingState, $jdk")
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
          val errorMsg = loadingError ?: EduJVMBundle.message("error.jdk.loading.failed", ENVIRONMENT_CONFIGURATION_LINK_JAVA)
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
