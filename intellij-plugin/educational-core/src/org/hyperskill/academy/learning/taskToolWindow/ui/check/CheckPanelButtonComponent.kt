package org.hyperskill.academy.learning.taskToolWindow.ui.check

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.util.ui.JBUI
import org.apache.commons.lang3.StringUtils
import org.hyperskill.academy.learning.actions.ActionWithProgressIcon
import org.hyperskill.academy.learning.ui.isDefault
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Panel with button inside and progress icon if action is appropriate
 */
class CheckPanelButtonComponent private constructor() : JPanel(BorderLayout()) {
  /**
   * @param[action] action to execute when button is clicked. Panel will also have process icon when action is being executed.
   * @param[isDefault] parameter specifies whether button is painted as default or not. `false` by default.
   * @param[isEnabled] parameter for enabling/disabling button. `true` by default.
   *
   * @see org.hyperskill.academy.learning.actions.ActionWithProgressIcon
   */
  constructor(action: ActionWithProgressIcon, isDefault: Boolean = false, isEnabled: Boolean = true) : this() {
    val buttonPanel = createButtonPanel(action = action, isDefault = isDefault, isEnabled = isEnabled)
    add(buttonPanel, BorderLayout.WEST)

    val spinnerPanel = action.spinnerPanel
    if (spinnerPanel != null) {
      add(spinnerPanel, BorderLayout.CENTER)
    }
  }

  /**
   * @param[action] action to execute when button is clicked.
   * @param[isDefault] parameter specifies whether button is painted as default or not. `false` by default.
   * @param[isEnabled] parameter for enabling/disabling button. `true` by default.
   */
  constructor(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    customButtonText: String? = null
  ) : this() {
    val buttonPanel =
      createButtonPanel(action, isDefault = isDefault, isEnabled = isEnabled, customButtonText = customButtonText)
    add(buttonPanel)
  }

  private fun createButtonPanel(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    customButtonText: String? = null
  ): JPanel {
    val button = createButton(action, isDefault = isDefault, isEnabled = isEnabled, customButtonText = customButtonText)
    return createButtonPanel(button)
  }

  private fun createButton(
    action: AnAction,
    isDefault: Boolean = false,
    isEnabled: Boolean = true,
    customButtonText: String? = null
  ): JButton {
    val text = if (customButtonText != null) StringUtils.abbreviate(customButtonText, 25) else action.templatePresentation.text
    val button = JButton(text).apply {
      this.isEnabled = isEnabled
      this.isFocusable = isEnabled
      this.isDefault = isDefault
    }
    if (isEnabled) {
      button.addActionListener { e ->
        ActionManager.getInstance().tryToExecute(
          action,
          null,
          this,
          CheckPanel.ACTION_PLACE,
          true
        )
      }
    }
    return button
  }

  private fun createButtonPanel(button: JComponent): JPanel {
    val buttonPanel = JPanel(GridLayout(1, 1, 5, 0))
    buttonPanel.add(button)
    val gridBagPanel = JPanel(GridBagLayout())
    val buttonPanelGridBagConstraints = GridBagConstraints(
      0, 0, 1, 1, 1.0, 0.0,
      GridBagConstraints.CENTER,
      GridBagConstraints.NONE,
      JBUI.insetsTop(8), 0, 0
    )
    gridBagPanel.add(buttonPanel, buttonPanelGridBagConstraints)
    return gridBagPanel
  }
}
