package org.hyperskill.academy.learning.taskToolWindow.ui.tab

import org.hyperskill.academy.learning.messages.BUNDLE
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.jetbrains.annotations.PropertyKey

enum class TabType(@param:PropertyKey(resourceBundle = BUNDLE) private val nameId: String) {
  DESCRIPTION_TAB("description.tab.name"),
  THEORY_TAB("hyperskill.theory.tab.name"),
  TOPICS_TAB("hyperskill.topics.tab.name"),
  SUBMISSIONS_TAB("submissions.tab.name");

  val tabName: String get() = EduCoreBundle.message(nameId)
}