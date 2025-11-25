package org.hyperskill.academy.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer

class RecentPreviewCourseListener : ProjectManagerListener, AppLifecycleListener {

  override fun projectClosing(project: Project) {
    if (!isUnitTestMode) {
      YamlFormatSynchronizer.saveAll(project)
    }
  }
}
