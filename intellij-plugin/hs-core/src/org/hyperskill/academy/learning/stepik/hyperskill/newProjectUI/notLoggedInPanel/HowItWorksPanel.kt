package org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel

import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.messages.EduCoreBundle
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JPanel


private const val ICON_TEXT_GAP = 32
private const val TOP_AND_BOTTOM_GAP = 41
private const val MIN_PANEL_WIDTH = 672
private const val MIN_PANEL_HEIGHT = 223
private const val MAX_TEXT_LENGTH = 24
private const val MIN_CARD_WIDTH = 216
private const val MIN_CARD_HEIGHT = 200
private const val TITLE_FONT_FACTOR = 3f

class HowItWorksPanel : JPanel(BorderLayout()) {

  init {
    isOpaque = false
    add(panel {
      row {
        text(EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works")).bold().applyToComponent {
          font = JBFont.regular().biggerOn(TITLE_FONT_FACTOR).deriveFont(Font.PLAIN)
        }
      }
      separator()
      // Cards container that wraps to the next line when width is insufficient.
      // Use WrapFlowLayout to calculate preferred height for multiple rows.
      row {
        val cardsPanel = JPanel(WrapFlowLayout(FlowLayout.LEFT, JBUI.scale(ICON_TEXT_GAP), JBUI.scale(ICON_TEXT_GAP))).apply {
          isOpaque = false
          add(
            HowItWorksCard(
              "selectCourseDialog/hyperskill/Features/select-learning-track.png",
              EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.learning.course.title"),
              EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.learning.course.description")
            )
          )
          add(
            HowItWorksCard(
              "selectCourseDialog/hyperskill/Features/learn-by-doing.png",
              EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.learn.by.doing.title"),
              EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.learn.by.doing.description")
            )
          )
          add(
            HowItWorksCard(
              "selectCourseDialog/hyperskill/Features/create-real-world-apps.png",
              EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.real.apps.title"),
              EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.real.apps.description"),
              EduCoreBundle.message("course.dialog.hyperskill.jba.on.hyperskill.how.it.works.real.apps.comment")
            )
          )
        }
        cell(cardsPanel).align(AlignX.FILL)
      }
    }.apply {
      isOpaque = false
      minimumSize = JBDimension(MIN_PANEL_WIDTH, MIN_PANEL_HEIGHT)
      border = JBUI.Borders.empty(TOP_AND_BOTTOM_GAP, 0)
    })
  }

  private class HowItWorksCard(
    iconPath: String,
    title: String,
    description: String,
    comment: String = ""
  ) : Wrapper() {

    init {
      val icon = IconUtil.resizeSquared(loadIcon(iconPath, HyperskillNotLoggedInPanel::class.java.classLoader), 48)

      setContent(panel {
        row {
          icon(icon)
        }
        row {
          text(title).bold()
        }
        row {
          text(description, MAX_TEXT_LENGTH)
        }
        row {
          comment(comment, MAX_TEXT_LENGTH)
        }
      }.apply {
        isOpaque = false
        minimumSize = JBDimension(MIN_CARD_WIDTH, MIN_CARD_HEIGHT)
      })
    }
  }

  /**
   * FlowLayout that calculates preferred/minimum size taking into account wrapping to multiple rows
   * when there is not enough width. This prevents clipping of rows beyond the first one.
   */
  private class WrapFlowLayout(align: Int, hgap: Int, vgap: Int) : FlowLayout(align, hgap, vgap) {
    override fun preferredLayoutSize(target: java.awt.Container): java.awt.Dimension = layoutSize(target, true)
    override fun minimumLayoutSize(target: java.awt.Container): java.awt.Dimension = layoutSize(target, false)

    private fun layoutSize(target: java.awt.Container, preferred: Boolean): java.awt.Dimension {
      val maxWidth = target.parent?.let { it.width }?.takeIf { it > 0 }
                     ?: target.width.takeIf { it > 0 }
                     ?: Int.MAX_VALUE

      val insets = target.insets
      val horizontalInsetsAndGap = insets.left + insets.right + hgap * 2
      val maxContentWidth = maxWidth - horizontalInsetsAndGap

      var rowWidth = 0
      var rowHeight = 0
      var totalWidth = 0
      var totalHeight = insets.top + insets.bottom + vgap // top padding; vgap before first row for symmetry

      val nComp = target.componentCount
      for (i in 0 until nComp) {
        val comp = target.getComponent(i)
        if (!comp.isVisible) continue
        val d = if (preferred) comp.preferredSize else comp.minimumSize

        if (rowWidth == 0) {
          // first component in the row
          rowWidth = d.width
          rowHeight = d.height
        }
        else if (rowWidth + hgap + d.width <= maxContentWidth) {
          // fits in current row
          rowWidth += hgap + d.width
          rowHeight = maxOf(rowHeight, d.height)
        }
        else {
          // new row
          totalWidth = maxOf(totalWidth, rowWidth)
          totalHeight += rowHeight + vgap
          rowWidth = d.width
          rowHeight = d.height
        }
      }

      // add last row
      totalWidth = maxOf(totalWidth, rowWidth)
      totalHeight += rowHeight

      // add insets and gaps
      totalWidth += insets.left + insets.right + hgap * 2
      return java.awt.Dimension(totalWidth, totalHeight)
    }
  }
}