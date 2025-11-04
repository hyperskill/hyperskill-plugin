package org.hyperskill.academy.learning.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Timer
import javax.swing.border.CompoundBorder

/**
 * The implementation has been adopted from the Intellij's platform [com.intellij.ui.InlineBanner] so to support adding custom actions.
 */
open class HintInlineBanner(
  protected val task: Task,
  @Nls messageText: String,
  status: Status = Status.Success,
  gap: Int = JBUI.scale(8)
) : InlineBannerBase(status.toNotificationBannerStatus(), gap, messageText) {
  private var closeAction: Runnable? = null
  private val linkActionPanel: JPanel = JPanel(HorizontalLayout(JBUI.scale(16)))
  private val iconActionPanel: JPanel = JPanel(HorizontalLayout(JBUI.scale(16)))
  private val actionsPanel: JPanel = JPanel(BorderLayout())

  init {
    val myCloseButton = createInplaceCloseButton {
      close()
    }

    layout = object : BorderLayout(gap, gap) {
      @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
      override fun addLayoutComponent(name: String?, comp: Component) {
        if (comp !== myCloseButton) {
          super.addLayoutComponent(name, comp)
        }
      }

      override fun layoutContainer(target: Container) {
        super.layoutContainer(target)
        val y = JBUI.scale(7)
        var x = target.width - JBUI.scale(7)
        if (myCloseButton.isVisible) {
          val size = myCloseButton.preferredSize
          x -= size.width
          myCloseButton.setBounds(x, y, size.width, size.height)
          x -= JBUI.scale(2)
        }
      }
    }

    setIcon()
    add(iconPanel, BorderLayout.WEST)
    add(centerPanel)
    add(myCloseButton)
    setTitle()
    setActions()
    isOpaque = false
    toolTipText = status.toolTipText
    border = createBorder(status.borderColor)
    background = status.backgroundColor
  }

  final override fun add(comp: Component?): Component {
    return super.add(comp)
  }

  final override fun add(comp: Component, constraints: Any?) {
    super.add(comp, constraints)
  }

  enum class Status(val backgroundColor: Color, val borderColor: Color, val toolTipText: String? = null) {
    Success(
      EduColors.hintInlineBannersBackgroundColor,
      EduColors.hintInlineBannersBorderColor,
      EduCoreBundle.message("hints.label.ai.generated.content.tooltip")
    ),
    Error(JBUI.CurrentTheme.Banner.ERROR_BACKGROUND, JBUI.CurrentTheme.Banner.ERROR_BORDER_COLOR, null);

    fun toNotificationBannerStatus(): EditorNotificationPanel.Status = when (this) {
      Success -> EditorNotificationPanel.Status.Info
      else -> EditorNotificationPanel.Status.Error
    }
  }

  private fun createBorder(color: Color): CompoundBorder = BorderFactory.createCompoundBorder(
    RoundedLineBorder(
      color, NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get()
    ), JBUI.Borders.empty(BORDER_OFFSET)
  )

  fun addAction(name: @Nls String, action: Runnable): InlineBannerBase {
    actionsPanel.isVisible = true
    linkActionPanel.add(object : LinkLabel<Runnable>(name, null, { _, action -> action.run() }, action) {
      override fun getTextColor() = JBUI.CurrentTheme.Link.Foreground.ENABLED
    }, linkActionPanel.componentCount - 1)
    return this
  }

  private fun setIcon() {
    val myIcon = JBLabel()
    myIcon.icon = EducationalCoreIcons.Actions.Hint
    myIcon.isVisible = true
    iconPanel.isVisible = true
    iconPanel.add(myIcon, BorderLayout.NORTH)
  }

  private fun setTitle() {
    val titlePanel = JPanel(BorderLayout())
    titlePanel.isOpaque = isOpaque
    titlePanel.background = background
    titlePanel.add(message)
    centerPanel.add(titlePanel)

    val myButtonPanel = JPanel()
    myButtonPanel.preferredSize = JBDimension(22, 16)
    myButtonPanel.isOpaque = isOpaque
    titlePanel.add(myButtonPanel, BorderLayout.EAST)
  }

  private fun setActions() {
    linkActionPanel.isOpaque = isOpaque
    iconActionPanel.isOpaque = isOpaque
    actionsPanel.isOpaque = isOpaque
    actionsPanel.add(linkActionPanel, BorderLayout.WEST)
    actionsPanel.add(iconActionPanel, BorderLayout.EAST)
    centerPanel.add(actionsPanel)
  }

  fun close() {
    closeAction?.run()
    removeFromParent()
  }

  private fun removeFromParent() {
    val parent = parent
    parent?.remove(this)
    parent?.doLayout()
    parent?.revalidate()
    parent?.repaint()
  }

  private fun createInplaceCloseButton(listener: ActionListener): JComponent {
    val button = object : InplaceButton(IdeBundle.message("editor.banner.close.tooltip"), IconButton(null, AllIcons.General.Close, null, null), listener) {
      private val myTimer = Timer(300) { stopClickTimer() }
      private var myClick = false

      private fun startClickTimer() {
        myClick = true
        repaint()
        myTimer.start()
      }

      private fun stopClickTimer() {
        myClick = false
        repaint()
        myTimer.stop()
      }

      override fun doClick(e: MouseEvent) {
        startClickTimer()
        super.doClick(e)
      }

      override fun paintHover(g: Graphics) {
        paintHover(g, if (myClick) JBUI.CurrentTheme.InlineBanner.PRESSED_BACKGROUND else JBUI.CurrentTheme.InlineBanner.HOVER_BACKGROUND)
      }
    }
    button.preferredSize = JBDimension(26, 26)
    button.isVisible = true
    return button
  }

  companion object {
    private const val BORDER_OFFSET: Int = 10
  }
}