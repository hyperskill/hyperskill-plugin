package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class RecentPreviewCourseListener : ProjectManagerListener, AppLifecycleListener {

  override fun projectClosing(project: Project) {
    if (!isUnitTestMode && project.isStudentProject()) {
      YamlFormatSynchronizer.saveAll(project)
    }
  }
}
