package org.hyperskill.academy.python.learning

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.python.sdk.setAssociationToModule

// Note: unlike previous versions, this file does not provide `installRequiredPackages(reporter, packageManager, requirements)`:
// it had no callers, and the APIs it relied on (`PythonPackageManager.installPackage`, `repositoryManager`, `toInstallRequest`)
// became internal in 262. Package installation goes through `PyEduUtils.installRequiredPackages(project, sdk)` instead.

internal fun setAssociationToModule(sdk: Sdk, module: Module) {
  runWithModalProgressBlocking(module.project, "") {
    sdk.setAssociationToModule(module)
  }
}
