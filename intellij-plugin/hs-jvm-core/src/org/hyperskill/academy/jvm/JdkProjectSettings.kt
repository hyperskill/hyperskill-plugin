package org.hyperskill.academy.jvm

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.LanguageLevelProjectExtension
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ProjectStructureConfigurable
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.DefaultSettingsUtils.findPath
import org.hyperskill.academy.learning.DefaultSettingsUtils.propertyValue
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.EduProjectSettings

open class JdkProjectSettings(val model: ProjectSdksModel, val jdk: Sdk?) : EduProjectSettings {

  fun setUpProjectJdk(
    project: Project,
    course: Course,
    getJdk: JdkProjectSettings.() -> Sdk? = { jdk }
  ): Sdk? {
    // Providing a JDK reaches deep into the platform -- it may scan a JDK home under a modal progress, or download a
    // whole JDK -- and anything thrown there used to cost the project its language level as well as its JDK, because
    // both are written by the same write action below.
    val jdk = try {
      ensureSuitableJdk(project, course, getJdk())?.let { registerJdk(it) }
    }
    catch (e: Throwable) {
      LOG.warn("Failed to provide a JDK for ${course.name}", e)
      null
    }

    return runWriteAction {
      // A project stores the *name* of its JDK and resolves it back through `ProjectJdkTable`, so only a registered
      // JDK is worth storing -- and `null` never is: [ProjectJdkRepair] runs concurrently on a freshly generated
      // project, and overwriting the JDK it just installed with nothing is how a course ends up with "No SDK".
      if (jdk != null) {
        ProjectRootManager.getInstance(project).projectSdk = jdk
      }
      val sdkVersion = course.requiredJdkVersion
      if (sdkVersion is JavaVersionParseSuccess) {
        LanguageLevelProjectExtension.getInstance(project).languageLevel = sdkVersion.javaSdkVersion.maxLanguageLevel
      }
      // Annotations are a nicety: `SdkModificator.commitChanges` may fail on an SDK backed by the workspace model, and
      // that must not cost the project the JDK and the language level that are already committed above.
      try {
        addAnnotations(ProjectRootManager.getInstance(project).projectSdk?.sdkModificator)
      }
      catch (e: Throwable) {
        LOG.warn("Failed to attach JDK annotations", e)
      }
      // Not `jdk`: the JDK the project ended up with is what the caller has to set the Gradle JVM from
      ProjectRootManager.getInstance(project).projectSdk
    }
  }

  /**
   * Returns a JDK the course can actually be built and checked with.
   *
   * [selectedJdk] may be missing or too old: the course dialog lets the learner start anyway when the required JDK
   * can be downloaded, and it is not shown at all on some paths. Look for an installed JDK that fits first, and
   * download the required one only when there is none, so the learner never lands on a project whose very first
   * check fails with "please update your SDK".
   */
  private fun ensureSuitableJdk(project: Project, course: Course, selectedJdk: Sdk?): Sdk? {
    // Courses that do not state a JVM version take whatever the learner picked, as they always did
    val requiredVersion = course.requiredJdkVersion as? JavaVersionParseSuccess ?: return selectedJdk
    if (JdkLanguageSettings.isSuitableJdk(selectedJdk, requiredVersion)) return selectedJdk

    val installedJdk = JdkLanguageSettings.findSuitableJdk(requiredVersion, model)
                       ?: JdkLanguageSettings.findSuitableJdkFromTable(requiredVersion)
    if (installedJdk != null) {
      LOG.info("Replaced ${selectedJdk?.name} with already installed ${installedJdk.name} required by the course")
      return installedJdk
    }

    val downloadedJdk = JdkAutoInstaller.installJdk(project, requiredVersion.javaSdkVersion)
    if (downloadedJdk == null) {
      LOG.warn("Failed to provide JDK ${requiredVersion.javaSdkVersion.description} required by the course, falling back to ${selectedJdk?.name}")
      return selectedJdk
    }
    return downloadedJdk
  }

  /**
   * Returns [jdk] as an SDK the whole IDE knows about, or `null` when it could not be registered.
   *
   * The course dialog offers candidates that are not registered anywhere yet -- the IDE's own runtime and a JDK being
   * downloaded, for two -- because building a real SDK goes through `SdkType.setupSdkPaths`, which scans the JDK home
   * under a modal progress and must not run while the dialog is up. This is where such a candidate becomes a real SDK.
   *
   * Failure is reported as `null` rather than by handing [jdk] back: a project resolves its JDK by name through
   * [ProjectJdkTable], so an SDK that never reached that table reads as "no SDK" everywhere in the IDE, and storing it
   * only hides the failure.
   *
   * `ProjectSdksModel.apply` is deliberately not used for this: the model also holds copies of the SDKs that are
   * already in [ProjectJdkTable], and committing those again throws `SymbolicIdAlreadyExistsException` on 2025.3+.
   */
  private fun registerJdk(jdk: Sdk): Sdk? {
    val homePath = jdk.homePath ?: return null
    findRegisteredJdk(homePath)?.let { return it }

    val registered = try {
      SdkConfigurationUtil.createAndAddSDK(homePath, JavaSdk.getInstance())
    }
    catch (e: Throwable) {
      LOG.warn("Failed to register the JDK located at $homePath", e)
      null
    }
    // `SdkConfigurationUtil` reports most of its failures by returning `null` after a warning of its own, and it may
    // well have added the JDK and only failed to scan its roots, so look it up once more before giving up.
    return registered ?: findRegisteredJdk(homePath).also {
      if (it == null) LOG.warn("JDK located at $homePath was not added to the JDK table")
    }
  }

  private fun findRegisteredJdk(homePath: String): Sdk? =
    ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance()).find { FileUtil.pathsEqual(it.homePath, homePath) }

  private fun addAnnotations(sdkModificator: SdkModificator?) {
    sdkModificator?.apply {
      JavaSdkImpl.attachJdkAnnotations(this)
      commitChanges()
    }
  }

  companion object {

    private val LOG = logger<JdkProjectSettings>()

    private const val DEFAULT_JDK_PROPERTY: String = "project.jdk"
    private const val DEFAULT_JDK_NAME_PROPERTY: String = "project.jdk.name"

    private const val DEFAULT_JDK_NAME: String = "jdk"

    fun emptySettings(): JdkProjectSettings {
      val configurable = ProjectStructureConfigurable.getInstance(ProjectManager.getInstance().defaultProject)
      return JdkProjectSettings(configurable.projectJdksModel, null)
    }

    fun defaultSettings(): Result<JdkProjectSettings, String> {
      // Use `EnvironmentService` instead to get default JDK path and name
      return findPath(DEFAULT_JDK_PROPERTY, "jdk").flatMap { jdkPath ->
        val jdkName = propertyValue(DEFAULT_JDK_NAME_PROPERTY, "JDK name").onError { DEFAULT_JDK_NAME }

        var jdk = ProjectJdkTable.getInstance().findJdk(jdkName)
        val jdkAlreadyExists = jdk != null

        if (jdk == null) {
          val jdkHomeDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(jdkPath)
          if (jdkHomeDir == null) {
            return@flatMap Err("$jdkPath doesn't exist")
          }
          jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), jdkHomeDir, JavaSdk.getInstance(), true, null, jdkName)
          if (jdk == null) {
            return@flatMap Err("Failed to create JDK for $jdkPath")
          }
        }

        val sdksModel = ProjectSdksModel()
        // Only add SDK to model if it doesn't already exist in ProjectJdkTable
        // to avoid SymbolicIdAlreadyExistsException when model.apply() is called
        if (!jdkAlreadyExists) {
          sdksModel.addSdk(jdk)
        }

        Ok(JdkProjectSettings(sdksModel, jdk))
      }
    }
  }
}
