@file:JvmName("JsUtils")

package org.hyperskill.academy.javascript.learning

import com.intellij.javascript.nodejs.npm.InstallNodeLocalDependenciesAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.invokeLater

const val NodeJS = "Node.js"

fun installNodeDependencies(project: Project, packageJsonFile: VirtualFile) {
  project.invokeLater { InstallNodeLocalDependenciesAction.runAndShowConsole(project, packageJsonFile) }
}