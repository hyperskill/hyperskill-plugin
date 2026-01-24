package org.hyperskill.academy.learning.framework.debug

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
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
import java.awt.Component
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * ToolWindow for debugging Framework Storage.
 * Shows commits, changed files, and diffs.
 * Only visible in internal/development mode.
 */
class FrameworkStorageToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    if (!project.isEduProject()) return

    val panel = FrameworkStoragePanel(project)
    val content = toolWindow.contentManager.factory.createContent(panel, "Commits", false)
    content.setDisposer(panel)
    toolWindow.contentManager.addContent(content)
  }

  override fun shouldBeAvailable(project: Project): Boolean {
    return ApplicationManager.getApplication().isInternal && project.isEduProject()
  }
}

/**
 * Main panel with three-pane layout: commits | files | diff
 */
class FrameworkStoragePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

  private val LOG = Logger.getInstance(FrameworkStoragePanel::class.java)

  private val listModel = DefaultListModel<CommitEntry>()
  private val commitList = JBList(listModel)
  private val dateFormat = SimpleDateFormat("dd MMM HH:mm:ss")

  private val fileTreeRoot = DefaultMutableTreeNode("Changes")
  private val fileTreeModel = DefaultTreeModel(fileTreeRoot)
  private val fileTree = Tree(fileTreeModel)

  private val diffPanel = JPanel(BorderLayout())
  private val diffPlaceholder = JBLabel("Select a file to view diff", SwingConstants.CENTER)

  private var autoRefresh = true
  private var currentStorage: FrameworkStorage? = null
  private var currentCommitSnapshot: Map<String, String> = emptyMap()
  private var parentCommitSnapshot: Map<String, String> = emptyMap()

  // Stage selector
  private val stageComboBox = JComboBox<StageItem>()
  private var isUpdatingStageComboBox = false

  // Header labels
  private val storageInfoLabel = JLabel()
  private val headInfoLabel = JLabel()

  // All commits data for filtering
  private var allCommitEntries: List<CommitEntry> = emptyList()
  private var commitsByRef: Map<Int, Set<String>> = emptyMap() // refId -> reachable commit hashes
  private var lastKnownHeadRefId: Int = -1 // Track HEAD to detect changes

  init {
    setupCommitList()
    setupFileTree()
    setupDiffPanel()
    setupStageSelector()
    setupLayout()
    subscribeToStorageChanges()
    refreshList()
  }

  private fun setupStageSelector() {
    stageComboBox.renderer = StageItemRenderer()
    stageComboBox.addActionListener {
      if (!isUpdatingStageComboBox) {
        filterCommitsBySelectedStage()
      }
    }
  }

  private fun setupCommitList() {
    commitList.cellRenderer = CommitListCellRenderer()
    commitList.fixedCellHeight = -1 // Auto-size based on content
    commitList.selectionMode = ListSelectionModel.SINGLE_SELECTION
    commitList.addListSelectionListener(ListSelectionListener {
      if (!it.valueIsAdjusting) {
        onCommitSelected(commitList.selectedValue)
      }
    })

    // Context menu
    commitList.addMouseListener(object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) = handlePopup(e)
      override fun mouseReleased(e: MouseEvent) = handlePopup(e)

      private fun handlePopup(e: MouseEvent) {
        if (e.isPopupTrigger) {
          val index = commitList.locationToIndex(e.point)
          if (index >= 0) {
            commitList.selectedIndex = index
            showCommitContextMenu(e)
          }
        }
      }
    })
  }

  private fun showCommitContextMenu(e: MouseEvent) {
    val entry = commitList.selectedValue ?: return

    val actionGroup = DefaultActionGroup().apply {
      add(object : AnAction("Copy Commit Hash", "Copy full commit hash to clipboard", AllIcons.Actions.Copy) {
        override fun actionPerformed(e: AnActionEvent) {
          CopyPasteManager.getInstance().setContents(StringSelection(entry.hash))
        }
      })
    }

    val popupMenu = ActionManager.getInstance().createActionPopupMenu("FrameworkStorageCommitPopup", actionGroup)
    popupMenu.component.show(commitList, e.x, e.y)
  }

  private fun setupFileTree() {
    fileTree.isRootVisible = false
    fileTree.cellRenderer = FileTreeCellRenderer()
    fileTree.addTreeSelectionListener(TreeSelectionListener {
      val node = fileTree.lastSelectedPathComponent as? DefaultMutableTreeNode
      val fileChange = node?.userObject as? FileChange
      if (fileChange != null) {
        showDiff(fileChange)
      }
    })
  }

  private fun setupDiffPanel() {
    diffPanel.add(diffPlaceholder, BorderLayout.CENTER)
  }

  private fun setupLayout() {
    // Left panel: commits list
    val commitsPanel = JPanel(BorderLayout()).apply {
      // Header
      val headerPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(4, 8)
        add(storageInfoLabel)
        add(headInfoLabel)
      }

      // Toolbar
      val toolbar = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = JBUI.Borders.empty(4)

        add(JLabel("Stage: "))
        add(Box.createHorizontalStrut(4))
        add(stageComboBox)
        add(Box.createHorizontalStrut(8))
        add(JButton(AllIcons.Actions.Refresh).apply {
          toolTipText = "Refresh"
          addActionListener { refreshList() }
        })
        add(Box.createHorizontalStrut(4))
        add(JCheckBox("Auto", autoRefresh).apply {
          toolTipText = "Auto-refresh on changes"
          addActionListener { autoRefresh = isSelected }
        })
        add(Box.createHorizontalGlue())
      }

      val topPanel = JPanel(BorderLayout()).apply {
        add(toolbar, BorderLayout.NORTH)
        add(headerPanel, BorderLayout.CENTER)
      }

      add(topPanel, BorderLayout.NORTH)
      add(JBScrollPane(commitList), BorderLayout.CENTER)
    }

    // Middle panel: file tree
    val filesPanel = JPanel(BorderLayout()).apply {
      border = JBUI.Borders.customLine(JBColor.border(), 0, 1, 0, 1)
      add(JBLabel("Changed Files").apply {
        border = JBUI.Borders.empty(4, 8)
      }, BorderLayout.NORTH)
      add(JBScrollPane(fileTree), BorderLayout.CENTER)
    }

    // Right panel: diff
    val rightPanel = JPanel(BorderLayout()).apply {
      add(JBLabel("Diff").apply {
        border = JBUI.Borders.empty(4, 8)
      }, BorderLayout.NORTH)
      add(diffPanel, BorderLayout.CENTER)
    }

    // Create splitters
    val rightSplitter = OnePixelSplitter(false, 0.35f).apply {
      firstComponent = filesPanel
      secondComponent = rightPanel
    }

    val mainSplitter = OnePixelSplitter(false, 0.3f).apply {
      firstComponent = commitsPanel
      secondComponent = rightSplitter
    }

    add(mainSplitter, BorderLayout.CENTER)
  }

  private fun subscribeToStorageChanges() {
    val connection = project.messageBus.connect(this)
    connection.subscribe(FrameworkStorageListener.TOPIC, object : FrameworkStorageListener {
      override fun snapshotSaved(refId: Int, commitHash: String) {
        if (autoRefresh) invokeLater { refreshList() }
      }

      override fun headUpdated(refId: Int) {
        if (autoRefresh) invokeLater { refreshList() }
      }
    })
  }

  fun refreshList() {
    listModel.clear()
    clearFileTree()
    clearDiff()

    val storagePath = Paths.get(FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistory", "storage"))
    currentStorage?.dispose()
    currentStorage = try {
      FrameworkStorage(storagePath)
    } catch (e: Exception) {
      storageInfoLabel.text = "Error: ${e.message}"
      headInfoLabel.text = ""
      return
    }

    val storage = currentStorage ?: return

    val version = storage.version
    val headRefId = storage.head
    val headCommitHash = storage.getHeadCommit()

    storageInfoLabel.text = "Storage v$version"
    headInfoLabel.text = if (headRefId != -1) "HEAD → ref/$headRefId" else "HEAD: not set"

    val taskNames = getTaskNames()
    LOG.warn("ToolWindow: taskNames = $taskNames")
    val refs = storage.getAllRefs()
    LOG.warn("ToolWindow: refs from storage = ${refs.map { "ref ${it.refId} -> ${it.commitHash.take(7)}" }}")

    // Build ref map
    val refsByCommit = mutableMapOf<String, MutableList<RefLabel>>()
    for (ref in refs) {
      val label = RefLabel(
        refId = ref.refId,
        taskName = taskNames[ref.refId] ?: "Stage ${ref.refId}",
        isHead = ref.isHead
      )
      refsByCommit.getOrPut(ref.commitHash) { mutableListOf() }.add(label)
    }

    // Collect commits and build reachability map per ref
    val allCommits = mutableMapOf<String, CommitData>()
    val reachableByRef = mutableMapOf<Int, MutableSet<String>>()

    fun collectCommitsForRef(hash: String, refId: Int, visited: MutableSet<String>, depth: Int = 0) {
      if (hash in visited || depth > 50) return
      visited.add(hash)
      reachableByRef.getOrPut(refId) { mutableSetOf() }.add(hash)
      val commit = storage.getCommit(hash) ?: return
      allCommits[hash] = CommitData(hash, commit)
      for (parentHash in commit.parentHashes) {
        collectCommitsForRef(parentHash, refId, visited, depth + 1)
      }
    }

    for (ref in refs) {
      collectCommitsForRef(ref.commitHash, ref.refId, mutableSetOf())
    }

    commitsByRef = reachableByRef

    // Build all commit entries
    val sortedCommits = allCommits.values.sortedByDescending { it.commit.timestamp }
    allCommitEntries = sortedCommits.map { commitData ->
      CommitEntry(
        hash = commitData.hash,
        timestamp = commitData.commit.timestamp,
        parentHashes = commitData.commit.parentHashes,
        snapshotHash = commitData.commit.snapshotHash,
        refs = refsByCommit[commitData.hash] ?: emptyList(),
        message = commitData.commit.message
      )
    }

    // Update stage selector
    updateStageSelector(refs, taskNames, headRefId)

    // Apply filter
    filterCommitsBySelectedStage()
  }

  private fun updateStageSelector(
    refs: List<FileBasedFrameworkStorage.RefInfo>,
    taskNames: Map<Int, String>,
    headRefId: Int
  ) {
    isUpdatingStageComboBox = true
    try {
      val previousSelection = stageComboBox.selectedItem as? StageItem
      val headChanged = lastKnownHeadRefId != headRefId && lastKnownHeadRefId != -1
      lastKnownHeadRefId = headRefId

      stageComboBox.removeAllItems()

      // Add "All" option
      stageComboBox.addItem(StageItem.All)

      // Get all tasks from course in order
      val course = StudyTaskManager.getInstance(project).course
      val frameworkLesson = course?.lessons?.filterIsInstance<FrameworkLesson>()?.firstOrNull()
      val refIdSet = refs.map { it.refId }.toSet()

      val stageItems = if (frameworkLesson != null) {
        // Show all tasks from course in order
        frameworkLesson.taskList.mapIndexed { index, task ->
          val stageNumber = index + 1
          val hasStorage = task.record != -1 && task.record in refIdSet
          StageItem.Stage(
            refId = task.record,
            name = "$stageNumber. ${task.name}",
            isHead = task.record == headRefId,
            hasStorage = hasStorage
          )
        }
      } else {
        // Fallback: show refs from storage
        refs.sortedBy { it.refId }.map { ref ->
          StageItem.Stage(
            refId = ref.refId,
            name = taskNames[ref.refId] ?: "Stage ${ref.refId}",
            isHead = ref.refId == headRefId,
            hasStorage = true
          )
        }
      }

      for (item in stageItems) {
        stageComboBox.addItem(item)
      }

      // If HEAD changed, follow it; otherwise restore previous selection
      val itemToSelect = when {
        headChanged -> stageItems.find { it.isHead } ?: StageItem.All
        previousSelection == StageItem.All -> StageItem.All
        previousSelection != null && stageItems.any { it.refId == (previousSelection as? StageItem.Stage)?.refId } ->
          stageItems.find { it.refId == (previousSelection as? StageItem.Stage)?.refId }
        else -> stageItems.find { it.isHead } ?: stageItems.firstOrNull { it.hasStorage } ?: StageItem.All
      }
      stageComboBox.selectedItem = itemToSelect
    } finally {
      isUpdatingStageComboBox = false
    }
  }

  private fun filterCommitsBySelectedStage() {
    listModel.clear()

    val selectedItem = stageComboBox.selectedItem as? StageItem ?: return

    val filteredCommits = when (selectedItem) {
      is StageItem.All -> allCommitEntries
      is StageItem.Stage -> {
        if (selectedItem.refId == -1 || !selectedItem.hasStorage) {
          emptyList() // No storage data for this stage
        } else {
          val reachableHashes = commitsByRef[selectedItem.refId] ?: emptySet()
          allCommitEntries.filter { it.hash in reachableHashes }
        }
      }
    }

    for (entry in filteredCommits) {
      listModel.addElement(entry)
    }
  }

  private fun onCommitSelected(entry: CommitEntry?) {
    clearFileTree()
    clearDiff()
    if (entry == null) return

    val storage = currentStorage ?: return

    try {
      // Load current commit's snapshot
      currentCommitSnapshot = loadSnapshot(storage, entry.snapshotHash)

      // Load parent's snapshot (if exists)
      parentCommitSnapshot = if (entry.parentHashes.isNotEmpty()) {
        val parentCommit = storage.getCommit(entry.parentHashes.first())
        if (parentCommit != null) {
          loadSnapshot(storage, parentCommit.snapshotHash)
        } else emptyMap()
      } else emptyMap()

      // Calculate changes
      val changes = calculateChanges(parentCommitSnapshot, currentCommitSnapshot)

      // Populate file tree
      populateFileTree(changes)

    } catch (e: Exception) {
      fileTreeRoot.add(DefaultMutableTreeNode("Error: ${e.message}"))
      fileTreeModel.reload()
    }
  }

  private fun loadSnapshot(storage: FrameworkStorage, snapshotHash: String): Map<String, String> {
    return storage.getSnapshotByHash(snapshotHash) ?: emptyMap()
  }

  private fun calculateChanges(oldState: Map<String, String>, newState: Map<String, String>): List<FileChange> {
    val changes = mutableListOf<FileChange>()

    // Added files
    for ((path, content) in newState) {
      if (path !in oldState) {
        changes.add(FileChange(path, ChangeType.ADDED, null, content))
      }
    }

    // Modified files
    for ((path, newContent) in newState) {
      val oldContent = oldState[path]
      if (oldContent != null && oldContent != newContent) {
        changes.add(FileChange(path, ChangeType.MODIFIED, oldContent, newContent))
      }
    }

    // Deleted files
    for ((path, oldContent) in oldState) {
      if (path !in newState) {
        changes.add(FileChange(path, ChangeType.DELETED, oldContent, null))
      }
    }

    return changes.sortedBy { it.path }
  }

  private fun populateFileTree(changes: List<FileChange>) {
    fileTreeRoot.removeAllChildren()

    if (changes.isEmpty()) {
      fileTreeRoot.add(DefaultMutableTreeNode("No changes"))
    } else {
      // Group by directory
      val byDir = changes.groupBy { it.path.substringBeforeLast('/', "") }

      for ((dir, files) in byDir.entries.sortedBy { it.key }) {
        if (dir.isEmpty()) {
          // Root level files
          for (file in files) {
            fileTreeRoot.add(DefaultMutableTreeNode(file))
          }
        } else {
          val dirNode = DefaultMutableTreeNode(dir)
          for (file in files) {
            dirNode.add(DefaultMutableTreeNode(file))
          }
          fileTreeRoot.add(dirNode)
        }
      }
    }

    fileTreeModel.reload()
    expandAllTreeNodes()
  }

  private fun expandAllTreeNodes() {
    var row = 0
    while (row < fileTree.rowCount) {
      fileTree.expandRow(row)
      row++
    }
  }

  private fun showDiff(fileChange: FileChange) {
    diffPanel.removeAll()

    val oldContent = fileChange.oldContent ?: ""
    val newContent = fileChange.newContent ?: ""

    try {
      val diffContentFactory = DiffContentFactory.getInstance()
      val fileType = FileTypeManager.getInstance().getFileTypeByFileName(fileChange.path)

      val oldDiffContent = diffContentFactory.create(project, oldContent, fileType)
      val newDiffContent = diffContentFactory.create(project, newContent, fileType)

      val title = when (fileChange.type) {
        ChangeType.ADDED -> "Added: ${fileChange.path}"
        ChangeType.MODIFIED -> "Modified: ${fileChange.path}"
        ChangeType.DELETED -> "Deleted: ${fileChange.path}"
      }

      val request = SimpleDiffRequest(title, oldDiffContent, newDiffContent, "Before", "After")
      val diffComponent = DiffManager.getInstance().createRequestPanel(project, this, null)
      diffComponent.setRequest(request)

      diffPanel.add(diffComponent.component, BorderLayout.CENTER)
    } catch (e: Exception) {
      // Fallback to simple text view
      val textArea = JTextArea().apply {
        isEditable = false
        text = buildString {
          appendLine("=== ${fileChange.path} (${fileChange.type}) ===")
          appendLine()
          if (fileChange.oldContent != null) {
            appendLine("--- OLD ---")
            appendLine(fileChange.oldContent)
            appendLine()
          }
          if (fileChange.newContent != null) {
            appendLine("+++ NEW +++")
            appendLine(fileChange.newContent)
          }
        }
      }
      diffPanel.add(JBScrollPane(textArea), BorderLayout.CENTER)
    }

    diffPanel.revalidate()
    diffPanel.repaint()
  }

  private fun clearFileTree() {
    fileTreeRoot.removeAllChildren()
    fileTreeModel.reload()
    currentCommitSnapshot = emptyMap()
    parentCommitSnapshot = emptyMap()
  }

  private fun clearDiff() {
    diffPanel.removeAll()
    diffPanel.add(diffPlaceholder, BorderLayout.CENTER)
    diffPanel.revalidate()
    diffPanel.repaint()
  }

  private fun getTaskNames(): Map<Int, String> {
    val course = StudyTaskManager.getInstance(project).course ?: return emptyMap()
    val result = mutableMapOf<Int, String>()
    for (lesson in course.lessons) {
      if (lesson !is FrameworkLesson) continue
      for (task in lesson.taskList) {
        if (task.record != -1) {
          result[task.record] = task.name
        }
      }
    }
    return result
  }

  override fun dispose() {
    currentStorage?.dispose()
  }

  // Data classes
  enum class ChangeType { ADDED, MODIFIED, DELETED }
  data class RefLabel(val refId: Int, val taskName: String, val isHead: Boolean)
  data class CommitData(val hash: String, val commit: FileBasedFrameworkStorage.Commit)

  sealed class StageItem {
    data object All : StageItem()
    data class Stage(val refId: Int, val name: String, val isHead: Boolean, val hasStorage: Boolean = true) : StageItem()
  }
  data class CommitEntry(
    val hash: String,
    val timestamp: Long,
    val parentHashes: List<String>,
    val snapshotHash: String,
    val refs: List<RefLabel>,
    val message: String
  )
  data class FileChange(
    val path: String,
    val type: ChangeType,
    val oldContent: String?,
    val newContent: String?
  )

  /**
   * Cell renderer for stage selector.
   */
  inner class StageItemRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>?,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

      when (val item = value as? StageItem) {
        is StageItem.All -> {
          text = "All stages"
          icon = AllIcons.Vcs.Branch
        }
        is StageItem.Stage -> {
          val suffix = when {
            item.isHead -> " (HEAD)"
            !item.hasStorage -> " (no data)"
            else -> ""
          }
          text = "${item.name}$suffix"
          icon = when {
            item.isHead -> AllIcons.Actions.Checked
            !item.hasStorage -> AllIcons.General.Warning
            else -> AllIcons.Nodes.Tag
          }
          if (!item.hasStorage && !isSelected) {
            foreground = JBColor.GRAY
          }
        }
        null -> {}
      }

      return this
    }
  }

  /**
   * Cell renderer for commit list.
   */
  inner class CommitListCellRenderer : ColoredListCellRenderer<CommitEntry>() {
    override fun customizeCellRenderer(
      list: JList<out CommitEntry>,
      value: CommitEntry?,
      index: Int,
      selected: Boolean,
      hasFocus: Boolean
    ) {
      if (value == null) return

      icon = AllIcons.Vcs.CommitNode

      // Refs
      for (ref in value.refs) {
        if (ref.isHead) {
          append(" HEAD ", headBadgeAttributes())
          append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
        append(" ${ref.taskName} ", if (ref.isHead) headRefBadgeAttributes() else refBadgeAttributes())
        append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
      }

      // Hash
      append(value.hash.take(7), commitHashAttributes())
      append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)

      // Time
      append(dateFormat.format(Date(value.timestamp)), SimpleTextAttributes.GRAYED_ATTRIBUTES)

      // Message
      if (value.message.isNotBlank()) {
        append(" - ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        append(value.message, SimpleTextAttributes.REGULAR_ATTRIBUTES)
      }
    }

    private fun headBadgeAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_BOLD, JBColor(0xFFFFFF, 0xFFFFFF), JBColor(0x9065AF, 0xB07FCC)
    )

    private fun headRefBadgeAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_BOLD, JBColor(0xFFFFFF, 0xFFFFFF), JBColor(0x1A7F37, 0x3FB950)
    )

    private fun refBadgeAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN, JBColor(0xFFFFFF, 0xFFFFFF), JBColor(0x6E7781, 0x6E7781)
    )

    private fun commitHashAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN, JBColor(0xCF222E, 0xF85149)
    )
  }

  /**
   * Cell renderer for file tree.
   */
  inner class FileTreeCellRenderer : ColoredTreeCellRenderer() {
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
          icon = if (leaf) AllIcons.General.Information else AllIcons.Nodes.Folder
          append(node, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
        is FileChange -> {
          val fileName = node.path.substringAfterLast('/')
          icon = when (node.type) {
            ChangeType.ADDED -> AllIcons.General.Add
            ChangeType.MODIFIED -> AllIcons.Actions.Edit
            ChangeType.DELETED -> AllIcons.General.Remove
          }
          val attrs = when (node.type) {
            ChangeType.ADDED -> SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(0x1A7F37, 0x3FB950))
            ChangeType.MODIFIED -> SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(0x9A6700, 0xD29922))
            ChangeType.DELETED -> SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor(0xCF222E, 0xF85149))
          }
          append(fileName, attrs)
        }
      }
    }
  }
}
