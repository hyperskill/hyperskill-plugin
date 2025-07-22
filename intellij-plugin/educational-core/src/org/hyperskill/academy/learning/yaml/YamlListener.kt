package org.hyperskill.academy.learning.yaml

import com.intellij.openapi.vfs.VirtualFile

interface YamlListener {
  fun beforeYamlLoad(configFile: VirtualFile)
  fun yamlFailedToLoad(configFile: VirtualFile, exception: String)
}