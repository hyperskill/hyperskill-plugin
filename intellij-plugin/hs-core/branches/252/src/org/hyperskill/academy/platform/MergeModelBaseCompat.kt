package org.hyperskill.academy.platform

import com.intellij.diff.merge.MergeModelBase
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project

abstract class MergeModelBaseCompat(
  project: Project,
  document: Document,
) : MergeModelBase<MergeModelBase.State>(project, document) {
  override fun reinstallHighlighters(index: Int) {}
}
