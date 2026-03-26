package org.hyperskill.academy.cpp

import com.jetbrains.cidr.cpp.toolchains.CMakeExecutableTool

// BACKCOMPAT: 252. In 2025.2 getBundledCMakeToolBinary takes (Boolean, ToolKind) parameters
internal fun makeCmakeExecutable() {
  @Suppress("DEPRECATION")
  val cmakeFile = CMakeExecutableTool.getBundledCMakeToolBinary(false, CMakeExecutableTool.ToolKind.CMAKE)
  if (cmakeFile.exists() && !cmakeFile.canExecute()) {
    cmakeFile.setExecutable(true)
  }
}
