package org.hyperskill.academy.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.management.PythonPackageManager
import com.jetbrains.python.packaging.management.toInstallRequest
import com.jetbrains.python.sdk.setAssociationToModule

// In 253, findPackageSpecificationWithVersionSpec was moved from PythonPackageManager
// to PythonRepositoryManager and renamed to findPackageSpecification
internal suspend fun installRequiredPackages(
  reporter: SequentialProgressReporter,
  packageManager: PythonPackageManager,
  requirements: List<PyRequirement>
) {
  for (pyRequirement in requirements) {
    reporter.itemStep(pyRequirement.name) {
      val packageSpecification = packageManager.repositoryManager.findPackageSpecification(pyRequirement)
        ?: return@itemStep
      packageManager.installPackage(packageSpecification.toInstallRequest())
    }
  }
}

internal fun setAssociationToModule(sdk: Sdk, module: Module) {
  runWithModalProgressBlocking(module.project, "") {
    sdk.setAssociationToModule(module)
  }
}
