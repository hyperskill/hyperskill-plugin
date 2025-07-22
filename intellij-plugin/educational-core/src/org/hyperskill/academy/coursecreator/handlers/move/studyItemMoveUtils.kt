@file:JvmName("StudyItemMoveUtils")

package org.hyperskill.academy.coursecreator.handlers.move

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.coursecreator.ui.CCMoveStudyItemDialog
import org.hyperskill.academy.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

private var MOCK: MoveStudyItemUI? = null

/** Returns delta */
fun showMoveStudyItemDialog(project: Project, itemType: StudyItemType, thresholdName: String): Int? {
  val ui = if (isUnitTestMode) {
    MOCK ?: error("Mock UI should be set via `withMockMoveStudyItemUI`")
  }
  else {
    DialogMoveStudyItemUI()
  }
  return ui.showDialog(project, itemType, thresholdName)
}

@TestOnly
fun withMockMoveStudyItemUI(mockUi: MoveStudyItemUI, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  }
  finally {
    MOCK = null
  }
}

interface MoveStudyItemUI {
  fun showDialog(project: Project, itemType: StudyItemType, thresholdName: String): Int?
}

class DialogMoveStudyItemUI : MoveStudyItemUI {
  override fun showDialog(project: Project, itemType: StudyItemType, thresholdName: String): Int? {
    val dialog = CCMoveStudyItemDialog(project, itemType, thresholdName)
    dialog.show()
    return if (dialog.exitCode != DialogWrapper.OK_EXIT_CODE) null else dialog.indexDelta
  }
}
