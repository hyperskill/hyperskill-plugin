package org.hyperskill.academy.learning.framework.debug

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.framework.FrameworkStorageListener
import org.hyperskill.academy.learning.framework.impl.FrameworkStorage
import org.hyperskill.academy.learning.framework.storage.FileBasedFrameworkStorage
import java.awt.BorderLayout
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * ToolWindow for debugging Framework Storage.
 * Shows the commit tree with refs (stages) and HEAD.
 * Only visible in internal/development mode.
 */
class FrameworkStorageToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    if (!project.isEduProject()) return

    val panel = FrameworkStoragePanel(project)
    val content = toolWindow.contentManager.factory.createContent(panel, "Commit Tree", false)
    content.setDisposer(panel)
    toolWindow.contentManager.addContent(content)
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    // Only show in internal mode and for edu projects
    return ApplicationManager.getApplication().isInternal && project.isEduProject()
  }
}

/**
 * Panel displaying the Framework Storage commit tree.
 * Automatically refreshes when storage changes.
 */
class FrameworkStoragePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

  private val tree: Tree
  private val rootNode = DefaultMutableTreeNode("Framework Storage")
  private val treeModel = DefaultTreeModel(rootNode)
  private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  private var autoRefresh = true

  init {
    tree = Tree(treeModel)
    tree.isRootVisible = true
    tree.cellRenderer = CommitTreeCellRenderer()

    val scrollPane = JBScrollPane(tree)
    scrollPane.border = JBUI.Borders.empty()

    // Toolbar with refresh button and auto-refresh toggle
    val toolbar = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
      border = JBUI.Borders.empty(4)

      val refreshButton = JButton("Refresh").apply {
        icon = AllIcons.Actions.Refresh
        addActionListener { refreshTree() }
      }
      add(refreshButton)

      add(Box.createHorizontalStrut(8))

      val autoRefreshCheckbox = JCheckBox("Auto-refresh", autoRefresh).apply {
        addActionListener { autoRefresh = isSelected }
      }
      add(autoRefreshCheckbox)

      add(Box.createHorizontalGlue())
    }

    add(toolbar, BorderLayout.NORTH)
    add(scrollPane, BorderLayout.CENTER)

    // Subscribe to storage changes
    subscribeToStorageChanges()

    refreshTree()
  }

  private fun subscribeToStorageChanges() {
    val connection = project.messageBus.connect(this)
    connection.subscribe(FrameworkStorageListener.TOPIC, object : FrameworkStorageListener {
      override fun snapshotSaved(refId: Int, commitHash: String) {
        if (autoRefresh) {
          invokeLater { refreshTree() }
        }
      }

      override fun headUpdated(refId: Int) {
        if (autoRefresh) {
          invokeLater { refreshTree() }
        }
      }
    })
  }

  fun refreshTree() {
    rootNode.removeAllChildren()

    val storagePath = Paths.get(FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistory", "storage"))
    val storage = try {
      FrameworkStorage(storagePath)
    } catch (e: Exception) {
      rootNode.add(DefaultMutableTreeNode(ErrorNode("Failed to open storage: ${e.message}")))
      treeModel.reload()
      return
    }

    try {
      // Add storage info
      val version = storage.version
      val head = storage.head
      val headCommit = storage.getHeadCommit()

      val infoNode = DefaultMutableTreeNode(InfoNode("Storage v$version"))
      rootNode.add(infoNode)

      // Add HEAD info
      val headNode = if (head != -1) {
        DefaultMutableTreeNode(HeadNode(head, headCommit?.take(8) ?: "???"))
      } else {
        DefaultMutableTreeNode(HeadNode(-1, "not set"))
      }
      rootNode.add(headNode)

      // Get task names from course
      val taskNames = getTaskNames()

      // Add refs (stages)
      val refs = storage.getAllRefs()
      if (refs.isEmpty()) {
        rootNode.add(DefaultMutableTreeNode(InfoNode("No refs yet")))
      } else {
        val refsNode = DefaultMutableTreeNode(InfoNode("Refs (${refs.size})"))
        rootNode.add(refsNode)

        // Build commit graph
        val commitMap = mutableMapOf<String, MutableList<FileBasedFrameworkStorage.RefInfo>>()
        for (ref in refs) {
          commitMap.getOrPut(ref.commitHash) { mutableListOf() }.add(ref)
        }

        // Group refs by their commits and show tree
        for (ref in refs.sortedBy { it.refId }) {
          val taskName = taskNames[ref.refId] ?: "Stage ${ref.refId}"
          val refNode = DefaultMutableTreeNode(
            RefNode(
              refId = ref.refId,
              taskName = taskName,
              commitHash = ref.commitHash,
              isHead = ref.isHead
            )
          )
          refsNode.add(refNode)

          // Add commit info as child
          val commitNode = DefaultMutableTreeNode(
            CommitNode(
              hash = ref.commitHash,
              snapshotHash = ref.commit.snapshotHash,
              parentHashes = ref.commit.parentHashes,
              timestamp = ref.commit.timestamp
            )
          )
          refNode.add(commitNode)

          // Add parent chain (up to 3 levels)
          addParentCommits(storage, ref.commit.parentHashes, commitNode, 3)
        }
      }

    } finally {
      storage.dispose()
    }

    treeModel.reload()
    expandAll()
  }

  private fun addParentCommits(
    storage: FrameworkStorage,
    parentHashes: List<String>,
    parentNode: DefaultMutableTreeNode,
    depth: Int
  ) {
    if (depth <= 0 || parentHashes.isEmpty()) return

    for (parentHash in parentHashes) {
      val commit = storage.getCommit(parentHash)
      if (commit != null) {
        val commitNode = DefaultMutableTreeNode(
          CommitNode(
            hash = parentHash,
            snapshotHash = commit.snapshotHash,
            parentHashes = commit.parentHashes,
            timestamp = commit.timestamp,
            isParent = true
          )
        )
        parentNode.add(commitNode)
        addParentCommits(storage, commit.parentHashes, commitNode, depth - 1)
      }
    }
  }

  private fun getTaskNames(): Map<Int, String> {
    val course = StudyTaskManager.getInstance(project).course ?: return emptyMap()
    val result = mutableMapOf<Int, String>()
    for (lesson in course.lessons) {
      if (lesson !is FrameworkLesson) continue
      for (task in lesson.taskList) {
        if (task.record != -1) {
          result[task.record] = "${lesson.name} / ${task.name}"
        }
      }
    }
    return result
  }

  private fun expandAll() {
    var row = 0
    while (row < tree.rowCount) {
      tree.expandRow(row)
      row++
    }
  }

  override fun dispose() {
    // Connection is automatically disposed via Disposer
  }

  // Node types for the tree
  data class InfoNode(val text: String)
  data class ErrorNode(val text: String)
  data class HeadNode(val refId: Int, val commitHash: String)
  data class RefNode(val refId: Int, val taskName: String, val commitHash: String, val isHead: Boolean)
  data class CommitNode(
    val hash: String,
    val snapshotHash: String,
    val parentHashes: List<String>,
    val timestamp: Long,
    val isParent: Boolean = false
  )

  /**
   * Custom cell renderer for the commit tree.
   */
  inner class CommitTreeCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(
      tree: JTree,
      value: Any?,
      selected: Boolean,
      expanded: Boolean,
      leaf: Boolean,
      row: Int,
      hasFocus: Boolean
    ) {
      val node = (value as? DefaultMutableTreeNode)?.userObject ?: return

      when (node) {
        is String -> {
          icon = AllIcons.Nodes.Folder
          append(node, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        }

        is InfoNode -> {
          icon = AllIcons.General.Information
          append(node.text, SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }

        is ErrorNode -> {
          icon = AllIcons.General.Error
          append(node.text, SimpleTextAttributes.ERROR_ATTRIBUTES)
        }

        is HeadNode -> {
          icon = AllIcons.Vcs.Branch
          append("HEAD", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
          append(" -> ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
          if (node.refId != -1) {
            append("ref/${node.refId}", headRefAttributes())
            append(" (${node.commitHash})", SimpleTextAttributes.GRAYED_ATTRIBUTES)
          } else {
            append(node.commitHash, SimpleTextAttributes.GRAYED_ATTRIBUTES)
          }
        }

        is RefNode -> {
          if (node.isHead) {
            icon = AllIcons.Debugger.Db_set_breakpoint
            append("* ", headRefAttributes())
          } else {
            icon = AllIcons.Vcs.BranchNode
          }
          append("ref/${node.refId}", if (node.isHead) headRefAttributes() else refAttributes())
          append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
          append(node.taskName, SimpleTextAttributes.REGULAR_ATTRIBUTES)
          append(" [${node.commitHash.take(8)}]", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }

        is CommitNode -> {
          icon = if (node.isParent) AllIcons.Vcs.History else AllIcons.Vcs.CommitNode
          val hashAttr = if (node.isParent) SimpleTextAttributes.GRAYED_ATTRIBUTES else commitHashAttributes()
          append(node.hash.take(8), hashAttr)
          append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
          append(dateFormat.format(Date(node.timestamp)), SimpleTextAttributes.GRAYED_ATTRIBUTES)
          if (node.parentHashes.isNotEmpty()) {
            append(" <- ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
            append(node.parentHashes.joinToString(", ") { it.take(8) }, SimpleTextAttributes.GRAYED_ATTRIBUTES)
          }
        }
      }
    }

    private fun headRefAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_BOLD,
      JBColor.namedColor("Git.Branch.Current", JBColor(0x1A7F37, 0x3FB950))
    )

    private fun refAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN,
      JBColor.namedColor("Git.Branch", JBColor(0x6E7781, 0x8B949E))
    )

    private fun commitHashAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN,
      JBColor.namedColor("Git.Commit.Hash", JBColor(0xCF222E, 0xF85149))
    )
  }
}
