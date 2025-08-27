package org.hyperskill.academy.coursecreator.yaml

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.yaml.YamlFormatSettings
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer

fun createConfigFiles(project: Project) {
  project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, true)
  YamlFormatSynchronizer.saveAll(project)
  FileDocumentManager.getInstance().saveAllDocuments()
  UIUtil.dispatchAllInvocationEvents()
}
