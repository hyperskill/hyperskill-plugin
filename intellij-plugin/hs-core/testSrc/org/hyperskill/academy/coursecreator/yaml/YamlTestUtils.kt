package org.hyperskill.academy.coursecreator.yaml

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import org.hyperskill.academy.learning.yaml.YamlFormatSettings
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer

fun createConfigFiles(project: Project) {
  project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, true)
  YamlFormatSynchronizer.saveAll(project)
  FileDocumentManager.getInstance().saveAllDocuments()
  // `saveAll` writes the files in `invokeLater`. `UIUtil.dispatchAllInvocationEvents` doesn't always run them: in the first
  // test of a JVM with empty IDE caches, they ran only after the test. This one releases the write-intent lock while it
  // dispatches events, and returns only after an `invokeLater` scheduled after them has run.
  PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
}
