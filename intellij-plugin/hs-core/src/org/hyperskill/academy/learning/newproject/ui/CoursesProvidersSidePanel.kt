package org.hyperskill.academy.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.coursesStorage.CourseDeletedListener
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorageBase
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CoursePanel
import org.hyperskill.academy.learning.newproject.ui.myCourses.MyCoursesProvider
import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.TypographyManager
import java.awt.*
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer

private const val PROVIDER_TOP_BOTTOM_OFFSET = 11
private const val PROVIDER_LEFT_OFFSET = 5
private const val ICON_TEXT_GAP = 8

class CoursesProvidersSidePanel(private val myCoursesProvider: MyCoursesProvider, disposable: Disposable) : JBScrollPane() {
  private val tree = createCourseProvidersTree()

  init {
    val panel = JPanel(BorderLayout())
    panel.add(tree, BorderLayout.CENTER)

    setViewportView(panel)
    // The panel width should exactly fit the content without wrapping or horizontal scrolling
    horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_NEVER
    // Height can exceed the available space — enable vertical scroll on demand
    verticalScrollBarPolicy = VERTICAL_SCROLLBAR_AS_NEEDED
    border = JBUI.Borders.customLine(CoursePanel.DIVIDER_COLOR, 0, 0, 0, 1)
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(CoursesStorageBase.COURSE_DELETED, object : CourseDeletedListener {
      override fun courseDeleted(course: JBACourseFromStorage) {
        // Recalculate panel width after content changes
        adjustWidthToContent()
        tree.repaint()
      }
    })

    // Adjust width after the tree is built/expanded and the component is added to the hierarchy
    SwingUtilities.invokeLater { adjustWidthToContent() }
  }

  private fun createCourseProvidersTree(): Tree {
    val root = DefaultMutableTreeNode("")
    val allCoursesNode = DefaultMutableTreeNode(EduCoreBundle.message("course.dialog.all.courses"))
    CoursesPlatformProviderFactory.allProviders.forEach { provider ->
      allCoursesNode.add(DefaultMutableTreeNode(provider))
    }
    root.add(allCoursesNode)
    root.add(DefaultMutableTreeNode(myCoursesProvider))

    return Tree(root).apply {
      isRootVisible = false
      rowHeight = 0 // force row to calculate size basing on its content
      showsRootHandles = false
      border = JBUI.Borders.empty()
      cellRenderer = ProviderWithIconCellRenderer()
      TreeUtil.expandAll(this)
      focusListeners.forEach { removeFocusListener(it) }
      treeExpansionListeners.forEach { removeTreeExpansionListener(it) }
      setSelectionRow(1)
    }
  }

  fun addTreeSelectionListener(listener: TreeSelectionListener) {
    tree.addTreeSelectionListener(listener)
  }

  /**
   * Adjusts the scroll panel width to the widest tree element
   * so that all text and icons are fully visible without wrapping or horizontal scrolling.
   */
  private fun adjustWidthToContent() {
    // Ensure the tree is expanded so row bounds are measured correctly
    TreeUtil.expandAll(tree)

    var maxRowRight = 0
    val rowCount = tree.rowCount
    for (row in 0 until rowCount) {
      val bounds = tree.getRowBounds(row) ?: continue
      val right = bounds.x + bounds.width
      if (right > maxRowRight) maxRowRight = right
    }

    // Add insets of the viewport border and the panel border
    val viewportInsets = viewportBorder?.getBorderInsets(this) ?: JBUI.emptyInsets()
    val borderInsets = border?.getBorderInsets(this) ?: JBUI.emptyInsets()
    // Include a conservative allowance for the vertical scrollbar width so content isn't cut on the right
    val vScrollBarWidth = if (verticalScrollBarPolicy == VERTICAL_SCROLLBAR_NEVER) 0 else (verticalScrollBar?.preferredSize?.width ?: 0)
    val safetyPad = JBUI.scale(2)
    val totalWidth =
      maxRowRight + viewportInsets.left + viewportInsets.right + borderInsets.left + borderInsets.right + vScrollBarWidth + safetyPad

    // Update preferred/minimum width of the scroll panel; height is not critical for BorderLayout.WEST
    val width = totalWidth.coerceAtLeast(0)
    val newSize = Dimension(width, preferredSize.height)
    preferredSize = newSize
    minimumSize = Dimension(width, minimumSize.height)

    // Request parent re-layout so BorderLayout accounts for the new WEST width
    revalidate()
  }

  private class ProviderWithIconCellRenderer : DefaultTreeCellRenderer() {
    private val component = JPanel(FlowLayout(FlowLayout.LEFT, ICON_TEXT_GAP, 0))
    private val textLabel = JBLabel()
    private val iconLabel = JBLabel()

    init {
      textLabel.font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.fontSize)
      component.border = JBUI.Borders.empty(PROVIDER_TOP_BOTTOM_OFFSET, 0)
      component.add(iconLabel)
      component.add(textLabel)
    }

    override fun getTreeCellRendererComponent(
      tree: JTree?,
      value: Any?,
      selected: Boolean,
      expanded: Boolean,
      leaf: Boolean,
      row: Int,
      hasFocus: Boolean
    ): Component {
      if (value is DefaultMutableTreeNode) {
        val userObject = value.userObject
        val tabName = if (userObject is CoursesPlatformProvider) userObject.name else userObject.toString()
        when (userObject) {
          is MyCoursesProvider, is String -> {
            val additionalText = (userObject as? MyCoursesProvider)?.getAdditionalText(selected) ?: ""
            textLabel.text = UIUtil.toHtml("<b>$tabName</b>$additionalText")
            iconLabel.icon = null
            component.border = JBUI.Borders.empty(PROVIDER_TOP_BOTTOM_OFFSET, 0)
          }

          is CoursesPlatformProvider -> {
            textLabel.text = tabName
            iconLabel.icon = userObject.icon
            component.border = JBUI.Borders.empty(PROVIDER_TOP_BOTTOM_OFFSET, PROVIDER_LEFT_OFFSET, PROVIDER_TOP_BOTTOM_OFFSET, 0)
          }
        }
        textLabel.foreground = UIUtil.getListForeground(selected, hasFocus)
      }

      return component
    }
  }
}