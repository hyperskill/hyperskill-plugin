package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.PresentableNodeDescriptor.ColoredFragment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.projectView.CourseViewUtils.testPresentation
import org.jetbrains.annotations.TestOnly

abstract class EduNode<T : StudyItem>(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  open val item: T?
) : PsiDirectoryNode(project, value, viewSettings) {

  init {
    myName = value.name
  }

  /**
   * The text this node wrote in [updateImpl], kept so that [postprocess] can tell whether something replaced it.
   * Both run within a single presentation update, so the value is never stale.
   */
  private var studyItemText: List<ColoredFragment> = emptyList()

  /**
   * The study item this node stands for, or `null` when the node only shows a directory.
   *
   * A node may hold an item without standing for it: a directory inside a task keeps the task around to decide which
   * of its children are visible, but is still shown as the directory it is.
   */
  protected open val presentedItem: T?
    get() = item

  /** What this node is called in the tree. */
  protected open val presentableName: String
    get() = presentedItem?.presentableName ?: directoryName()

  /** The name the project view would give this directory, honouring settings such as compacted middle packages. */
  protected fun directoryName(): String =
    ProjectViewDirectoryHelper.getInstance(myProject).getNodeName(settings, parentValue, value) ?: value.name

  override fun updateImpl(data: PresentationData) {
    data.clearText()
    val item = presentedItem
    // The name has to be written before anything that may fail. The platform prefills the presentation with the name
    // and the icon of the underlying directory, so leaving this method early shows the directory instead of the study
    // item, e.g. `src` instead of a task name when the task node points at the task source directory.
    // `JBColor.BLACK` is the theme's foreground in a dark theme and plain black in a light one
    data.addText(presentableName, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.BLACK))
    try {
      if (item != null) {
        data.setIcon(CourseViewUtils.getIcon(item))
      }
      additionalInfo?.let { data.addText(" $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
    }
    catch (e: ProcessCanceledException) {
      throw e
    }
    catch (e: Exception) {
      LOG.warn("Failed to build presentation for ${item?.itemType} `${item?.name}`", e)
    }
    studyItemText = data.coloredText.toList()
  }

  /**
   * Restores the study item's name after the project view decorators, which the platform runs on every node right
   * after [updateImpl].
   *
   * Every course view node is a [PsiDirectoryNode], so `GradleModuleDirectoryDecorator` (IDEA 2026.2 and later)
   * treats it as a plain directory: for a directory that is a Gradle module content root it calls `clearText()` and
   * renders `<directory> [<module>]`. A task node points at the task source directory, so the task name is replaced
   * with `src [main]`.
   *
   * This is why [updateImpl] is the only place that writes a node's text: a node building its presentation some other
   * way silently opts out of this protection.
   */
  override fun postprocess(presentation: PresentationData) {
    super.postprocess(presentation)
    val text = studyItemText
    if (text.isEmpty() || presentation.coloredText == text) return
    presentation.clearText()
    text.forEach { presentation.addText(it) }
  }

  open val additionalInfo: String?
    get() {
      return null
    }

  override fun hasProblemFileBeneath(): Boolean = false

  @TestOnly
  override fun getTestPresentation(): String? = testPresentation(this)

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
    return ProjectViewDirectoryHelper.getInstance(myProject)
      .getDirectoryChildren(value, settings, true, null)
      .mapNotNull { modifyChildNode(it) }
  }

  protected open fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? = childNode

  companion object {
    private val LOG: Logger = Logger.getInstance(EduNode::class.java)
  }
}
