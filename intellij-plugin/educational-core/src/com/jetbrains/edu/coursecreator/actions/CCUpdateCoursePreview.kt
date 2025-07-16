package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.nio.file.Path

data class PreviewInfo(
  /**
   * The path to an archive, from which the preview was loaded
   */
  val previewLoadedFrom: Path,

  /**
   * The base path of the project, from which the preview was created
   */
  val sourceProjectBasePath: String?
)


private val previewInfoKey = Key<PreviewInfo>("Edu.course.preview.info")

var Project.previewInfo: PreviewInfo?
  get() = getUserData(previewInfoKey)
  set(value) = putUserData(previewInfoKey, value)