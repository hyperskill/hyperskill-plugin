package org.hyperskill.academy.jvm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkTypeId
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.projectRoots.impl.SdkVersionUtil
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.SdkListItem
import com.intellij.openapi.roots.ui.configuration.SdkListModelBuilder
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTracker
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.jvm.messages.EduJVMBundle
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JAVA
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.project
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessage
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessageType
import org.jetbrains.jps.model.java.JdkVersionDetector
import java.awt.BorderLayout
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JPanel

private val LOG = logger<JdkLanguageSettings>()

/**
 * The JDK part of the course creation dialog.
 *
 * Everything here is synchronous, and deliberately so: the JDKs on offer come from [ProjectJdkTable], an in-memory
 * list, so collecting them costs nothing and the combo box works the moment the dialog opens. The platform calls that
 * used to force all of this into a background coroutine -- `ProjectSdksModel.reset` and
 * `ProjectSdksModel.addSdk(SdkType, home, callback)` -- are not used at all: each runs a modal progress of its own,
 * which under the already modal course dialog may never finish, and used to leave the learner looking at
 * "No SDK configured" with nothing to click.
 *
 * The dialog does not have to *guarantee* a JDK either, only to offer one. When nothing suitable is installed, the
 * version the course requires is downloaded while the project is generated (see [JdkProjectSettings.setUpProjectJdk]),
 * and an existing project gets the same treatment when it is opened (see [ProjectJdkRepair]).
 */
open class JdkLanguageSettings : LanguageSettings<JdkProjectSettings>() {

  protected var jdk: Sdk? = null

  /**
   * A model of our own, not the one behind `ProjectStructureConfigurable`: that one is an application-wide singleton
   * shared with the Project Structure dialog. Nothing is ever committed from this model -- whatever the learner ends
   * up with is registered by [JdkProjectSettings.setUpProjectJdk].
   */
  protected val sdkModel: ProjectSdksModel = ProjectSdksModel()

  /** Whether [sdkModel] has been filled at least once, so that [validate] does not rescan on every keystroke. */
  private var jdksLoaded: Boolean = false

  /**
   * Version of the runtime the IDE itself runs on, read from disk once. [validate] runs on every keystroke in the
   * location field, and the bundled runtime cannot change while the dialog is open.
   */
  private val bundledJdkVersion: JdkVersionDetector.JdkVersionInfo? by lazy(LazyThreadSafetyMode.NONE) {
    val homePath = PathManager.getBundledRuntimePath()
    // An IDE started by the Gradle IntelliJ plugin has no bundled runtime
    if (Files.isDirectory(Path.of(homePath))) SdkVersionUtil.getJdkVersionInfo(homePath) else null
  }

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val requiredVersion = requiredJdkVersion(course)
    reloadJdks(requiredVersion)

    val jdkComboBox = createJdkComboBox(course, requiredVersion)
    jdkComboBox.selectedJdk = jdk
    updateSelectedJdk(jdkComboBox.selectedJdk)
    jdkComboBox.addItemListener {
      updateSelectedJdk(jdkComboBox.selectedItem?.jdk)
    }

    // The learner may configure a JDK in Settings while the dialog is open, or uninstall the one it preselected
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(ProjectJdkTable.JDK_TABLE_TOPIC, object : ProjectJdkTable.Listener {
      override fun jdkAdded(addedJdk: Sdk) {
        if (addedJdk.sdkType !is JavaSdkType) return
        invokeLater(ModalityState.any()) { refreshJdks(jdkComboBox, requiredVersion) }
      }

      override fun jdkRemoved(removedJdk: Sdk) {
        if (removedJdk.sdkType !is JavaSdkType) return
        invokeLater(ModalityState.any()) {
          sdkModel.projectSdks.values.filter { it.name == removedJdk.name }.forEach { sdkModel.removeSdk(it) }
          if (jdk?.name == removedJdk.name) {
            jdk = null
          }
          refreshJdks(jdkComboBox, requiredVersion)
        }
      }
    })

    return listOf(LabeledComponent.create(jdkRow(course, jdkComboBox, requiredVersion, disposable), "JDK", BorderLayout.WEST))
  }

  /**
   * A combo box offering nothing but the JDK the course requires.
   *
   * Three separate filters are needed to get there, which is why the model builder is assembled by hand instead of
   * letting [JdkComboBox] do it: registered JDKs go through the SDK filter, the ones the platform detected on disk
   * have a list and a filter of their own, and the `Download JDK...` action would open a picker offering every version
   * there is. That action is dropped and replaced by [downloadJdkLink], which pins the version.
   */
  private fun createJdkComboBox(course: Course, requiredVersion: ParsedJavaVersion): JdkComboBox {
    val isJavaSdkType = { sdkType: SdkTypeId -> sdkType is JavaSdkType && !sdkType.isDependent }
    val modelBuilder = SdkListModelBuilder(
      course.project,
      sdkModel,
      isJavaSdkType,
      isJavaSdkType,
      // A JDK the course cannot be built with is not offered at all rather than rejected once it is picked, so there
      // is nothing to choose that does not work. One being downloaded has no home directory yet and is kept on
      // purpose: that is how the combo box shows the download running.
      { sdk -> isSuitableJdk(sdk, requiredVersion) || SdkDownloadTracker.getInstance().isDownloading(sdk) },
      { suggested -> matchesRequiredVersion(releaseFeatureVersion(suggested.version), requiredVersion) },
      { role -> role != SdkListItem.ActionRole.DOWNLOAD },
    )
    return JdkComboBox(course.project, modelBuilder) { newJdk ->
      // `Add JDK...` creates the JDK inside the model; it becomes the selection right away
      updateSelectedJdk(newJdk)
    }
  }

  private fun jdkRow(
    course: Course,
    jdkComboBox: JdkComboBox,
    requiredVersion: ParsedJavaVersion,
    disposable: CheckedDisposable
  ): JComponent {
    val downloadLink = downloadJdkLink(course, jdkComboBox, requiredVersion, disposable) ?: return jdkComboBox
    return JPanel(BorderLayout(JBUI.scale(8), 0)).apply {
      isOpaque = false
      add(jdkComboBox, BorderLayout.CENTER)
      add(downloadLink, BorderLayout.EAST)
    }
  }

  /**
   * Replaces the platform's `Download JDK...` action with one that can only download the required version.
   *
   * Returns `null` when the course pins no version, or when the IDE cannot download JDKs at all -- in both cases there
   * is nothing this link could do that the combo box does not already do.
   */
  private fun downloadJdkLink(
    course: Course,
    jdkComboBox: JdkComboBox,
    requiredVersion: ParsedJavaVersion,
    disposable: CheckedDisposable
  ): JComponent? {
    if (requiredVersion !is JavaVersionParseSuccess) return null
    val requiredFeatureVersion = requiredVersion.javaSdkVersion.featureVersion ?: return null
    val javaSdk = JavaSdk.getInstance()
    if (!JdkDownloadUi.isAvailable(javaSdk)) return null

    return ActionLink(EduJVMBundle.message("action.download.jdk", requiredVersion.javaSdkVersion.description)) {
      JdkDownloadUi.show(javaSdk, sdkModel, jdkComboBox, course.project, requiredFeatureVersion) { task ->
        sdkModel.setupInstallableSdk(javaSdk, task) { downloadedJdk ->
          jdkComboBox.reloadModel()
          jdkComboBox.selectedJdk = downloadedJdk
          updateSelectedJdk(downloadedJdk)
          // The callback above fires when the download is *scheduled*, not when it is over: until then the JDK home is
          // the empty directory the installer created up front and the version string is only the planned one, so
          // everything downstream would take an unfinished download for an installed JDK. Re-check when it lands.
          SdkDownloadTracker.getInstance().tryRegisterDownloadingListener(downloadedJdk, disposable, EmptyProgressIndicator()) {
            invokeLater(ModalityState.any()) { refreshJdks(jdkComboBox, requiredVersion) }
          }
        }
      }
    }
  }

  /**
   * Fills [sdkModel] with the JDKs the learner can pick from and preselects the one the course needs.
   *
   * JDKs whose home directory is gone are left out: [ProjectJdkTable] keeps an entry after its JDK has been
   * uninstalled, and starting a course on such an entry produces a project the Gradle integration refuses to sync.
   */
  private fun reloadJdks(requiredVersion: ParsedJavaVersion) {
    // A JDK being downloaded has no launcher in its home yet and is kept on purpose: that is how the combo box shows
    // the download running, and dropping it would take the learner's selection away mid-download.
    sdkModel.projectSdks.values
      .filter { !it.hasExistingHome && !SdkDownloadTracker.getInstance().isDownloading(it) }
      .forEach { sdkModel.removeSdk(it) }
    val knownHomes = sdkModel.projectSdks.values.mapNotNullTo(mutableSetOf()) { it.systemIndependentHome }

    val candidates = ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance()).filter { it.hasExistingHome } +
                     listOfNotNull(bundledJdk(requiredVersion))
    for (candidate in candidates) {
      val home = candidate.systemIndependentHome ?: continue
      if (knownHomes.add(home)) {
        sdkModel.addSdk(candidate)
      }
    }

    if (!isSuitableJdk(jdk, requiredVersion)) {
      jdk = findSuitableJdk(requiredVersion, sdkModel) ?: jdk
    }
    jdksLoaded = true
  }

  private fun refreshJdks(jdkComboBox: JdkComboBox, requiredVersion: ParsedJavaVersion) {
    reloadJdks(requiredVersion)
    jdkComboBox.reloadModel()
    if (jdkComboBox.selectedJdk != jdk) {
      jdkComboBox.selectedJdk = jdk
    }
    updateSelectedJdk(jdkComboBox.selectedJdk ?: jdk)
  }

  /**
   * The runtime the IDE itself runs on, offered as one more JDK.
   *
   * It counts exactly like any other JDK -- a Java 23 is a Java 23 wherever it came from -- so it is offered only when
   * its version fits the course, and that is decided by reading the version off the JDK home. Building a real [Sdk]
   * for it is left to [JdkProjectSettings.setUpProjectJdk]: that goes through `SdkType.setupSdkPaths`, which scans the
   * whole JDK under a modal progress and has no business running while the course dialog is up.
   */
  private fun bundledJdk(requiredVersion: ParsedJavaVersion): Sdk? {
    val versionInfo = bundledJdkVersion ?: return null
    val requiredFeatureVersion = requiredVersion.requiredFeatureVersion
    if (requiredFeatureVersion != null && versionInfo.version.feature < requiredFeatureVersion) return null

    val homePath = PathManager.getBundledRuntimePath()
    return ProjectJdkImpl(versionInfo.suggestedName(), JavaSdk.getInstance(), homePath, versionInfo.version.toString())
  }

  private fun updateSelectedJdk(selectedJdk: Sdk?) {
    if (jdk == selectedJdk) return
    jdk = selectedJdk
    LOG.info("Selected JDK: ${selectedJdk?.name} (${selectedJdk?.versionString})")
    notifyListeners()
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    course ?: return super.validate(null, courseLocation)

    val courseJavaVersion = requiredJdkVersion(course)
    if (courseJavaVersion is JavaVersionParseFailed) {
      return ready("error.unsupported.java.version", courseJavaVersion.versionAsText)
    }

    // The dialog validates before it asks for the components, and reading the JDK table costs nothing
    if (!jdksLoaded) {
      reloadJdks(courseJavaVersion)
    }

    if (isSuitableJdk(jdk, courseJavaVersion)) {
      return SettingsValidationResult.OK
    }

    jdkDownloadPlanned(courseJavaVersion)?.let { return it }

    // Downloading is switched off, so the learner has to install the JDK themselves. Which one is the only thing worth
    // saying: nothing else is selectable, so there is no "your JDK is too old" case left to report.
    val requiredVersion = courseJavaVersion as? JavaVersionParseSuccess ?: return ready("error.no.jdk")
    return ready("error.no.required.jdk", requiredVersion.javaSdkVersion.description)
  }

  private fun ready(messageId: String, vararg additionalSubstitution: String): SettingsValidationResult {
    val message = EduJVMBundle.message(messageId, *additionalSubstitution)
    return SettingsValidationResult.Ready(ValidationMessage(message, ENVIRONMENT_CONFIGURATION_LINK_JAVA))
  }

  /**
   * The JDK a course requires is downloaded automatically while the project is generated
   * (see [JdkProjectSettings.setUpProjectJdk]), so its absence is a warning the learner can start the course with
   * rather than an error they have to fix in the IDE settings by hand.
   *
   * Returns `null` when downloading is not an option, and the caller has to report a real error instead.
   */
  private fun jdkDownloadPlanned(courseJavaVersion: ParsedJavaVersion): SettingsValidationResult? {
    if (courseJavaVersion !is JavaVersionParseSuccess) return null
    if (!JdkAutoInstaller.isAvailable()) return null

    val message = EduJVMBundle.message("jdk.will.be.downloaded", courseJavaVersion.javaSdkVersion.description)
    return SettingsValidationResult.ReadyWithWarning(ValidationMessage(message, type = ValidationMessageType.WARNING))
  }

  /**
   * The JDK version this course has to be opened with. Taken from the environment settings, except for Hyperskill
   * courses, which all pin the same version (see [Course.requiredJdkVersion]).
   */
  protected open fun requiredJdkVersion(course: Course): ParsedJavaVersion = course.requiredJdkVersion

  override fun getSettings(): JdkProjectSettings = JdkProjectSettings(sdkModel, jdk)

  companion object {

    fun findSuitableJdk(courseSdkVersion: ParsedJavaVersion, sdkModel: ProjectSdksModel): Sdk? {
      return chooseSuitableJdk(sdkModel.sdks.filter { it.sdkType == JavaSdk.getInstance() }, courseSdkVersion)
    }

    /**
     * Finds a suitable JDK directly in [ProjectJdkTable], for the callers that have no [ProjectSdksModel] at hand:
     * project generation and [ProjectJdkRepair].
     */
    fun findSuitableJdkFromTable(courseSdkVersion: ParsedJavaVersion): Sdk? {
      return chooseSuitableJdk(ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance()), courseSdkVersion)
    }

    private fun chooseSuitableJdk(jdks: List<Sdk>, courseSdkVersion: ParsedJavaVersion): Sdk? =
      jdks.firstOrNull { isSuitableJdk(it, courseSdkVersion) }

    /**
     * Whether [jdk] is the one a course requiring [courseSdkVersion] has to be opened with.
     *
     * A JDK whose home directory is gone never counts: [ProjectJdkTable] keeps an entry after its JDK has been
     * uninstalled, and it still reports the version string it was registered with.
     */
    fun isSuitableJdk(jdk: Sdk?, courseSdkVersion: ParsedJavaVersion): Boolean {
      jdk ?: return false
      if (!jdk.hasExistingHome) return false
      return matchesRequiredVersion(jdk.releaseFeatureVersion, courseSdkVersion)
    }

    /**
     * Whether a JDK of [featureVersion] is the one [courseSdkVersion] asks for.
     *
     * The course pins its JDK rather than setting a lower bound: the generated Gradle scripts derive the Java
     * toolchain from the JDK the daemon runs on, so a newer one quietly changes what the learner's code is compiled
     * against, while Hyperskill's own tests run on the pinned version. A course stating no version takes any release
     * JDK, as it always did.
     */
    private fun matchesRequiredVersion(featureVersion: Int?, courseSdkVersion: ParsedJavaVersion): Boolean {
      val requiredFeatureVersion = courseSdkVersion.requiredFeatureVersion ?: return featureVersion != null
      return featureVersion == requiredFeatureVersion
    }

    private val ParsedJavaVersion.requiredFeatureVersion: Int?
      get() = (this as? JavaVersionParseSuccess)?.javaSdkVersion?.featureVersion

    private val Sdk.systemIndependentHome: String?
      get() = homePath?.let { FileUtil.toSystemIndependentName(it) }
  }
}
