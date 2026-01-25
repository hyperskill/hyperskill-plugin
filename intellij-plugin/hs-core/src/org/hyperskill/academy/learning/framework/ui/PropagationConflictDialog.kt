package org.hyperskill.academy.learning.framework.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.messages.EduCoreBundle
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.*

/**
 * Dialog showing file differences when navigating between framework lesson stages.
 * Helps user understand what will happen with Keep vs Replace choice.
 */
class PropagationConflictDialog(
  project: Project,
  private val currentTaskName: String,
  private val targetTaskName: String,
  private val currentState: Map<String, String>,
  private val targetState: Map<String, String>
) : DialogWrapper(project, true) {

  enum class Result { KEEP, REPLACE }

  var result: Result = Result.KEEP
    private set

  init {
    title = EduCoreBundle.message("propagation.dialog.title")
    setOKButtonText(EduCoreBundle.message("propagation.dialog.replace"))
    setCancelButtonText(EduCoreBundle.message("propagation.dialog.keep"))
    init()
  }

  override fun createCenterPanel(): JComponent {
    val panel = JPanel(BorderLayout(0, JBUI.scale(12)))
    panel.border = JBUI.Borders.empty(10)

    // Header explaining the situation
    val headerText = EduCoreBundle.message("propagation.dialog.header", currentTaskName, targetTaskName)
    val headerLabel = JBLabel("<html>$headerText</html>")
    panel.add(headerLabel, BorderLayout.NORTH)

    // File changes list
    val changesPanel = createChangesPanel()
    val scrollPane = JBScrollPane(changesPanel)
    scrollPane.preferredSize = JBUI.size(500, 250)
    scrollPane.border = JBUI.Borders.empty()
    panel.add(scrollPane, BorderLayout.CENTER)

    // Footer explaining the choices
    val footerPanel = JPanel()
    footerPanel.layout = BoxLayout(footerPanel, BoxLayout.Y_AXIS)
    footerPanel.border = JBUI.Borders.emptyTop(10)

    val keepExplanation = JBLabel("<html><b>${EduCoreBundle.message("propagation.dialog.keep")}:</b> ${EduCoreBundle.message("propagation.dialog.keep.explanation", targetTaskName)}</html>")
    val replaceExplanation = JBLabel("<html><b>${EduCoreBundle.message("propagation.dialog.replace")}:</b> ${EduCoreBundle.message("propagation.dialog.replace.explanation", currentTaskName, targetTaskName)}</html>")

    keepExplanation.alignmentX = Component.LEFT_ALIGNMENT
    replaceExplanation.alignmentX = Component.LEFT_ALIGNMENT

    footerPanel.add(keepExplanation)
    footerPanel.add(Box.createVerticalStrut(JBUI.scale(5)))
    footerPanel.add(replaceExplanation)

    panel.add(footerPanel, BorderLayout.SOUTH)

    return panel
  }

  private fun createChangesPanel(): JComponent {
    val panel = JPanel()
    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
    panel.border = JBUI.Borders.empty(5)

    val changes = calculateChanges()

    if (changes.isEmpty()) {
      panel.add(JBLabel(EduCoreBundle.message("propagation.dialog.no.changes")))
      return panel
    }

    for (change in changes) {
      val row = createChangeRow(change)
      row.alignmentX = Component.LEFT_ALIGNMENT
      panel.add(row)
      panel.add(Box.createVerticalStrut(JBUI.scale(4)))
    }

    return panel
  }

  private fun createChangeRow(change: FileChange): JComponent {
    val row = JPanel(BorderLayout(JBUI.scale(8), 0))
    row.isOpaque = false

    // Change type badge
    val badge = JBLabel(change.type.displayName)
    badge.foreground = change.type.color
    badge.font = badge.font.deriveFont(badge.font.size2D - 1)
    badge.border = JBUI.Borders.empty(2, 6)
    badge.isOpaque = true
    badge.background = change.type.backgroundColor

    // File name
    val fileName = JBLabel(change.path)

    // Stats (for modified files)
    val stats = if (change.type == ChangeType.MODIFIED && change.addedLines != null && change.removedLines != null) {
      JBLabel("<html><font color='#22863A'>+${change.addedLines}</font> <font color='#CB2431'>-${change.removedLines}</font></html>")
    } else {
      null
    }

    row.add(badge, BorderLayout.WEST)
    row.add(fileName, BorderLayout.CENTER)
    stats?.let { row.add(it, BorderLayout.EAST) }

    return row
  }

  private fun calculateChanges(): List<FileChange> {
    val changes = mutableListOf<FileChange>()

    // Files in current but not in target (will be added on Replace)
    for ((path, content) in currentState) {
      if (path !in targetState) {
        changes.add(FileChange(path, ChangeType.ADDED, content.lines().size, null))
      }
    }

    // Files in target but not in current (will be removed on Replace)
    for ((path, _) in targetState) {
      if (path !in currentState) {
        changes.add(FileChange(path, ChangeType.REMOVED, null, null))
      }
    }

    // Files in both with different content (will be modified on Replace)
    for ((path, currentContent) in currentState) {
      val targetContent = targetState[path] ?: continue
      if (currentContent != targetContent) {
        val (added, removed) = calculateLineDiff(targetContent, currentContent)
        changes.add(FileChange(path, ChangeType.MODIFIED, added, removed))
      }
    }

    return changes.sortedWith(compareBy({ it.type.ordinal }, { it.path }))
  }

  private fun calculateLineDiff(oldContent: String, newContent: String): Pair<Int, Int> {
    val oldLines = oldContent.lines().toSet()
    val newLines = newContent.lines().toSet()
    val added = (newLines - oldLines).size
    val removed = (oldLines - newLines).size
    return added to removed
  }

  override fun doOKAction() {
    result = Result.REPLACE
    super.doOKAction()
  }

  override fun doCancelAction() {
    result = Result.KEEP
    super.doCancelAction()
  }

  private data class FileChange(
    val path: String,
    val type: ChangeType,
    val addedLines: Int?,
    val removedLines: Int?
  )

  private enum class ChangeType(val displayName: String, val color: JBColor, val backgroundColor: JBColor) {
    ADDED("Added", JBColor(0x22863A, 0x85E89D), JBColor(0xDCFFE4, 0x28332E)),
    REMOVED("Removed", JBColor(0xCB2431, 0xF97583), JBColor(0xFFDCE0, 0x3D2327)),
    MODIFIED("Modified", JBColor(0xB08800, 0xFFEA7F), JBColor(0xFFF5B1, 0x3D3520))
  }

  companion object {
    /**
     * Shows the dialog and returns user's choice.
     */
    fun show(
      project: Project,
      currentTaskName: String,
      targetTaskName: String,
      currentState: Map<String, String>,
      targetState: Map<String, String>
    ): Result {
      val dialog = PropagationConflictDialog(project, currentTaskName, targetTaskName, currentState, targetState)
      dialog.show()
      return dialog.result
    }
  }
}
