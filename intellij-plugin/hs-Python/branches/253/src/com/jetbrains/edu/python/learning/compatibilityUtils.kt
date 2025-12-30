package org.hyperskill.academy.python.learning

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.toInstallRequest
import com.jetbrains.python.sdk.setAssociationToModule

private val LOG = logger<PythonPackageManager>()

// In 253, findPackageSpecificationWithVersionSpec was moved from PythonPackageManager
// to PythonRepositoryManager and renamed to findPackageSpecification
internal suspend fun installRequiredPackages(
  reporter: SequentialProgressReporter,
  packageManager: PythonPackageManager,
  requirements: List<PyRequirement>
) {
  for ((index, pyRequirement) in requirements.withIndex()) {
    val progressText = "Installing ${pyRequirement.name} (${index + 1}/${requirements.size})"
    reporter.itemStep(progressText) {
      val packageSpecification = packageManager.repositoryManager.findPackageSpecification(pyRequirement)
      if (packageSpecification != null) {
        // Install the package - this does NOT throw exceptions on failure!
        try {
          LOG.warn("compatibilityUtils: Before installPackage ${pyRequirement.name}")
          packageManager.installPackage(packageSpecification.toInstallRequest())
          LOG.warn("compatibilityUtils: After installPackage ${pyRequirement.name} - no exception thrown")

          // Since installPackage doesn't throw exceptions, we can't reliably detect failures here
          // The Python plugin just logs warnings but returns normally
          // We would need to check installed packages, but that property is protected
        } catch (e: Throwable) {
          LOG.warn("compatibilityUtils: Exception during installPackage: ${e.javaClass.name}: ${e.message}")
          throw RuntimeException("Failed to install ${pyRequirement.name}: ${e.message}", e)
        }
      } else {
        // Package not found in repository - might be URL or unsupported format
        val errorMessage = "Cannot find package specification for requirement: ${pyRequirement.presentableText}. " +
                          "It might be a URL-based requirement which requires installation via pip install -r requirements.txt"
        LOG.warn(errorMessage)
        throw IllegalStateException(errorMessage)
      }
    }
  }
}

internal fun setAssociationToModule(sdk: Sdk, module: Module) {
  runWithModalProgressBlocking(module.project, "") {
    sdk.setAssociationToModule(module)
  }
}
