package org.hyperskill.academy.csharp

import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.util.ui.tree.TreeUtil
import com.jetbrains.rider.projectView.views.impl.SolutionViewContextMenu
import org.hyperskill.academy.learning.projectView.CourseViewPaneCustomization
import javax.swing.JTree

class CSharpCourseViewPaneCustomization : CourseViewPaneCustomization {
  override fun customize(tree: JTree) {
    CustomizationUtil.installPopupHandler(tree, SolutionViewContextMenu.Id, ActionPlaces.PROJECT_VIEW_POPUP)
    TreeUtil.installActions(tree)
  }
}
