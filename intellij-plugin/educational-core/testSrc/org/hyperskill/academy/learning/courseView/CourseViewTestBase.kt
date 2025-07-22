package org.hyperskill.academy.learning.courseView

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ui.tree.TreeUtil
import org.hyperskill.academy.learning.EduActionTestCase
import org.hyperskill.academy.learning.projectView.CourseViewPane

abstract class CourseViewTestBase : EduActionTestCase() {
  protected fun assertCourseView(structure: String) {
    val tree = createPane().tree
    PlatformTestUtil.waitForPromise(TreeUtil.promiseExpandAll(tree))
    PlatformTestUtil.assertTreeEqual(tree, structure + "\n")
  }

  protected fun createPane(): CourseViewPane {
    val pane = CourseViewPane(project)
    pane.createComponent()
    Disposer.register(testRootDisposable, pane)
    PlatformTestUtil.waitWhileBusy(pane.tree)
    return pane
  }
}
