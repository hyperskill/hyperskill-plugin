package org.hyperskill.academy.cpp

import com.jetbrains.cidr.cpp.toolchains.CMakeExecutableTool

internal fun makeCmakeExecutable() {
  val cmakeFile = CMakeExecutableTool.getBundledCMakeToolBinary(CMakeExecutableTool.ToolKind.CMAKE)
  if (cmakeFile.exists() && !cmakeFile.canExecute()) {
    cmakeFile.setExecutable(true)
  }
}
