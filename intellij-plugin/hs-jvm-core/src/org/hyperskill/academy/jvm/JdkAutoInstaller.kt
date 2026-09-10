package org.hyperskill.academy.jvm

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkInstaller
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkListDownloader
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hyperskill.academy.jvm.messages.EduJVMBundle
import org.hyperskill.academy.learning.computeUnderProgress
import java.nio.file.Path

private val LOG = logger<JdkAutoInstaller>()

/**
 * Downloads a JDK of the feature version a course requires, from the same JetBrains JDK feed the
 * `Download JDK...` item of the IDE JDK combo box uses.
 *
 * The whole `com.intellij.openapi.projectRoots.impl.jdkDownloader` package is marked `@ApiStatus.Internal`, and
 * there is no public replacement: `SdkDownload.showDownloadUI` always opens a modal picker, and
 * `ProjectSdksModel.setupInstallableSdk` needs an `SdkDownloadTask` that only those internal classes can build.
 * The three types used here -- `JdkListDownloader`, `JdkInstaller` and `JdkInstallRequest` -- have identical
 * signatures on 252, 253, 261 and 262, so one source form compiles on every supported platform.
 */
object JdkAutoInstaller {

  /** Registry key the platform itself uses to switch the JDK downloader off. */
  private const val JDK_DOWNLOADER_REGISTRY_KEY = "jdk.downloader"

  /**
   * Whether a missing JDK may be downloaded. Never true in tests: they must not reach out to the network and
   * must not install anything on the machine that runs them.
   */
  fun isAvailable(): Boolean {
    if (ApplicationManager.getApplication().isUnitTestMode) return false
    return Registry.`is`(JDK_DOWNLOADER_REGISTRY_KEY, true)
  }

  /**
   * Downloads [javaSdkVersion] and registers it in [ProjectJdkTable], showing a progress dialog while it runs.
   * Returns `null` if the version is not offered by the feed or the download failed; in that case the caller
   * keeps whatever JDK it had.
   *
   * Must be called from the EDT: the download itself runs in the background, but registering the resulting SDK
   * needs a write action.
   */
  @RequiresEdt
  fun installJdk(project: Project?, javaSdkVersion: JavaSdkVersion): Sdk? {
    if (!isAvailable()) return null
    val featureVersion = javaSdkVersion.featureVersion ?: return null

    val javaHome = try {
      computeUnderProgress(project, EduJVMBundle.message("progress.downloading.jdk", javaSdkVersion.description)) {
        downloadJdk(project, featureVersion, it)
      }
    }
    catch (_: ProcessCanceledException) {
      // The learner cancelled the download. The project is already created at this point, so it must still open:
      // they keep the JDK they had and the course checker tells them to update it.
      LOG.info("Downloading JDK ${javaSdkVersion.description} was cancelled")
      null
    } ?: return null

    return try {
      registerJdk(javaHome)
    }
    catch (e: Throwable) {
      // Registering goes through `SdkConfigurationUtil`, which scans the JDK home under a modal progress of its own
      // and rethrows cancellation. Losing the JDK is bad; taking the rest of project generation down with it is worse.
      LOG.warn("Failed to register the JDK downloaded to $javaHome", e)
      null
    }
  }

  /**
   * Same as [installJdk], but for callers that are already inside a coroutine and must not block the EDT with a modal
   * progress -- opening a project, for instance. Reports through the usual background progress bar instead.
   */
  suspend fun installJdkInBackground(project: Project, javaSdkVersion: JavaSdkVersion): Sdk? {
    if (!isAvailable()) return null
    val featureVersion = javaSdkVersion.featureVersion ?: return null

    val title = EduJVMBundle.message("progress.downloading.jdk", javaSdkVersion.description)
    val javaHome = withBackgroundProgress(project, title, false) {
      withContext(Dispatchers.IO) { downloadJdk(project, featureVersion, EmptyProgressIndicator()) }
    } ?: return null

    return try {
      withContext(Dispatchers.EDT) { registerJdk(javaHome) }
    }
    catch (e: ProcessCanceledException) {
      // `SdkConfigurationUtil` deliberately rethrows the cancellation of the JDK scan it runs, and
      // `ProcessCanceledException` is a `CancellationException`, which the platform rethrows out of a startup activity
      // without logging anything at all. The download itself is done, so this is a failure to register, not a reason
      // to abort project opening in silence.
      LOG.warn("Registering the JDK downloaded to $javaHome was cancelled")
      null
    }
    catch (e: CancellationException) {
      throw e
    }
    catch (e: Throwable) {
      LOG.warn("Failed to register the JDK downloaded to $javaHome", e)
      null
    }
  }

  /**
   * Downloads a JDK with the given feature version and returns the path to its java home, or `null` on failure.
   * Performs network and disk I/O, so it must not be called on the EDT.
   */
  private fun downloadJdk(project: Project?, featureVersion: Int, indicator: ProgressIndicator): Path? {
    return try {
      val candidates = JdkListDownloader.getInstance()
        .downloadModelForJdkInstaller(indicator)
        .filter { it.jdkMajorVersion == featureVersion }
      // Take the vendor the IDE itself would suggest; the feed marks no default for versions that are not current,
      // so fall back to any build offered in the JDK picker
      val jdkItem = candidates.firstOrNull { it.isDefaultItem }
                    ?: candidates.firstOrNull { it.isVisibleOnUI }
                    ?: candidates.firstOrNull()
      if (jdkItem == null) {
        LOG.warn("JDK $featureVersion is not offered by the JDK feed, nothing to download")
        return null
      }

      val installer = JdkInstaller.getInstance()
      val request = installer.prepareJdkInstallation(jdkItem, installer.defaultInstallDir(jdkItem))
      LOG.info("Downloading ${jdkItem.fullPresentationText} into ${request.installDir}")
      installer.installJdk(request, indicator, project)
      request.javaHome
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Throwable) {
      LOG.warn("Failed to download JDK $featureVersion", e)
      null
    }
  }

  /**
   * Adds the JDK installed at [javaHome] to [ProjectJdkTable], reusing an entry that already points there.
   */
  @RequiresEdt
  private fun registerJdk(javaHome: Path): Sdk? {
    // `Path.toString` is system-dependent, the JDK table stores system-independent home paths
    val homePath = FileUtil.toSystemIndependentName(javaHome.toString())
    findRegisteredJdk(homePath)?.let { return it }

    // `SdkConfigurationUtil` reports most of its failures by returning `null` after a warning of its own, and it may
    // well have added the JDK and only failed to scan its roots, so look it up once more before giving up.
    val sdk = SdkConfigurationUtil.createAndAddSDK(homePath, JavaSdk.getInstance()) ?: findRegisteredJdk(homePath)
    if (sdk == null) {
      LOG.warn("Failed to register the downloaded JDK located at $homePath")
    }
    return sdk
  }

  private fun findRegisteredJdk(homePath: String): Sdk? =
    ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance()).find { FileUtil.pathsEqual(it.homePath, homePath) }
}
