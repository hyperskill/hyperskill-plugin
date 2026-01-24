package org.hyperskill.academy.learning.framework.debug

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.framework.FrameworkStorageListener
import org.hyperskill.academy.learning.framework.impl.FrameworkStorage
import org.hyperskill.academy.learning.framework.storage.FileBasedFrameworkStorage
import java.awt.BorderLayout
import java.awt.Component
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*

/**
 * ToolWindow for debugging Framework Storage.
 * Shows the commit list like VCS log with refs (stages) and HEAD.
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
 * Panel displaying the Framework Storage commits like VCS log.
 */
class FrameworkStoragePanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

  private val listModel = DefaultListModel<CommitEntry>()
  private val commitList = JBList(listModel)
  private val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm:ss")
  private var autoRefresh = true

  // Header labels
  private val storageInfoLabel = JLabel()
  private val headInfoLabel = JLabel()

  init {
    commitList.cellRenderer = CommitListCellRenderer()
    commitList.fixedCellHeight = 28

    val scrollPane = JBScrollPane(commitList)
    scrollPane.border = JBUI.Borders.empty()

    // Header panel with storage info
    val headerPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
      border = JBUI.Borders.empty(8, 8, 4, 8)

      add(storageInfoLabel)
      add(Box.createVerticalStrut(2))
      add(headInfoLabel)
    }

    // Toolbar
    val toolbar = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
      border = JBUI.Borders.empty(4)

      val refreshButton = JButton("Refresh").apply {
        icon = AllIcons.Actions.Refresh
        addActionListener { refreshList() }
      }
      add(refreshButton)

      add(Box.createHorizontalStrut(8))

      val autoRefreshCheckbox = JCheckBox("Auto-refresh", autoRefresh).apply {
        addActionListener { autoRefresh = isSelected }
      }
      add(autoRefreshCheckbox)

      add(Box.createHorizontalGlue())
    }

    val topPanel = JPanel(BorderLayout()).apply {
      add(toolbar, BorderLayout.NORTH)
      add(headerPanel, BorderLayout.CENTER)
    }

    add(topPanel, BorderLayout.NORTH)
    add(scrollPane, BorderLayout.CENTER)

    subscribeToStorageChanges()
    refreshList()
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

    val storagePath = Paths.get(FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistory", "storage"))
    val storage = try {
      FrameworkStorage(storagePath)
    } catch (e: Exception) {
      storageInfoLabel.text = "Error: ${e.message}"
      headInfoLabel.text = ""
      return
    }

    try {
      val version = storage.version
      val headRefId = storage.head
      val headCommitHash = storage.getHeadCommit()

      storageInfoLabel.text = "Storage v$version"
      headInfoLabel.text = if (headRefId != -1) {
        "HEAD → ref/$headRefId (${headCommitHash?.take(8) ?: "???"})"
      } else {
        "HEAD: not set"
      }

      val taskNames = getTaskNames()
      val refs = storage.getAllRefs()

      // Build ref map: commitHash -> list of refs pointing to it
      val refsByCommit = mutableMapOf<String, MutableList<RefLabel>>()
      for (ref in refs) {
        val label = RefLabel(
          refId = ref.refId,
          taskName = taskNames[ref.refId] ?: "Stage ${ref.refId}",
          isHead = ref.isHead
        )
        refsByCommit.getOrPut(ref.commitHash) { mutableListOf() }.add(label)
      }

      // Collect all unique commits and sort by timestamp (newest first)
      val allCommits = mutableMapOf<String, CommitData>()
      val visited = mutableSetOf<String>()

      fun collectCommits(hash: String, depth: Int = 0) {
        if (hash in visited || depth > 50) return
        visited.add(hash)

        val commit = storage.getCommit(hash) ?: return
        allCommits[hash] = CommitData(hash, commit)

        for (parentHash in commit.parentHashes) {
          collectCommits(parentHash, depth + 1)
        }
      }

      // Start from all refs
      for (ref in refs) {
        collectCommits(ref.commitHash)
      }

      // Sort by timestamp descending
      val sortedCommits = allCommits.values.sortedByDescending { it.commit.timestamp }

      // Add to list model
      for (commitData in sortedCommits) {
        val refs = refsByCommit[commitData.hash] ?: emptyList()
        listModel.addElement(
          CommitEntry(
            hash = commitData.hash,
            timestamp = commitData.commit.timestamp,
            parentHashes = commitData.commit.parentHashes,
            refs = refs
          )
        )
      }

    } finally {
      storage.dispose()
    }
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

  override fun dispose() {}

  // Data classes
  data class RefLabel(val refId: Int, val taskName: String, val isHead: Boolean)
  data class CommitData(val hash: String, val commit: FileBasedFrameworkStorage.Commit)
  data class CommitEntry(
    val hash: String,
    val timestamp: Long,
    val parentHashes: List<String>,
    val refs: List<RefLabel>
  )

  /**
   * Cell renderer for commit list - styled like VCS log.
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

      // Refs (branches/tags) - shown first like in git log
      for (ref in value.refs) {
        if (ref.isHead) {
          append(" HEAD ", headBadgeAttributes())
          append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
        }
        append(" ${ref.taskName} ", if (ref.isHead) headRefBadgeAttributes() else refBadgeAttributes())
        append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)
      }

      // Commit hash
      append(value.hash.take(8), commitHashAttributes())
      append(" ", SimpleTextAttributes.REGULAR_ATTRIBUTES)

      // Timestamp
      append(dateFormat.format(Date(value.timestamp)), SimpleTextAttributes.GRAYED_ATTRIBUTES)

      // Parent info
      if (value.parentHashes.isNotEmpty()) {
        append("  ←", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        for (parent in value.parentHashes) {
          append(" ${parent.take(7)}", parentHashAttributes())
        }
      }
    }

    private fun headBadgeAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_BOLD,
      JBColor.namedColor("Git.Log.Ref.LocalBranch", JBColor(0xFFFFFF, 0xFFFFFF)),
      JBColor.namedColor("Git.Log.Ref.LocalBranch.bg", JBColor(0x9065AF, 0xB07FCC))
    )

    private fun headRefBadgeAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_BOLD,
      JBColor.namedColor("Git.Log.Ref.LocalBranch", JBColor(0xFFFFFF, 0xFFFFFF)),
      JBColor.namedColor("Git.Log.Ref.LocalBranch.bg", JBColor(0x1A7F37, 0x3FB950))
    )

    private fun refBadgeAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN,
      JBColor.namedColor("Git.Log.Ref.RemoteBranch", JBColor(0xFFFFFF, 0xFFFFFF)),
      JBColor.namedColor("Git.Log.Ref.RemoteBranch.bg", JBColor(0x6E7781, 0x6E7781))
    )

    private fun commitHashAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN,
      JBColor.namedColor("Git.Commit.Hash", JBColor(0xCF222E, 0xF85149))
    )

    private fun parentHashAttributes() = SimpleTextAttributes(
      SimpleTextAttributes.STYLE_PLAIN,
      JBColor.namedColor("Git.Commit.Hash.Muted", JBColor(0x8B949E, 0x8B949E))
    )
  }
}
