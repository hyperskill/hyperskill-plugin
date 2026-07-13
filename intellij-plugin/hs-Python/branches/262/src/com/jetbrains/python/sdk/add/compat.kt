package com.jetbrains.python.sdk.add

import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.ComponentWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PySdkListCellRenderer
import com.jetbrains.python.sdk.PythonSdkType
import javax.swing.JTextField
import javax.swing.plaf.basic.BasicComboBoxEditor

/**
 * Minimal reimplementation of `com.jetbrains.python.sdk.add.PySdkPathChoosingComboBox`,
 * which was removed from the Python plugin in 262 together with the whole v1 "add SDK" UI.
 *
 * Combobox items are plain [Sdk] instances.
 */
class PySdkPathChoosingComboBox : ComponentWithBrowseButton<ComboBox<Any>>(ComboBox<Any>(), null) {

  private val busyIconExtension: ExtendableTextComponent.Extension =
    ExtendableTextComponent.Extension { AnimatedIcon.Default.INSTANCE }

  private val busyEditor: BasicComboBoxEditor = object : BasicComboBoxEditor() {
    override fun createEditorComponent(): JTextField = ExtendableTextField().apply { isEditable = false }
  }

  init {
    childComponent.renderer = PySdkListCellRenderer()
    addActionListener {
      val descriptor = PythonSdkType.getInstance().homeChooserDescriptor
      FileChooser.chooseFiles(descriptor, null, null) { chosenFiles ->
        val virtualFile = chosenFiles.firstOrNull() ?: return@chooseFiles
        val path = FileUtil.toSystemDependentName(virtualFile.path)
        childComponent.selectedItem = items.find { it.homePath == path } ?: PyDetectedSdk(path).also { addSdkItemOnTop(it) }
      }
    }
  }

  val selectedSdk: Sdk?
    get() = childComponent.selectedItem as? Sdk

  val items: List<Sdk>
    get() = (0 until childComponent.itemCount).mapNotNull { childComponent.getItemAt(it) as? Sdk }

  fun addSdkItem(sdk: Sdk) {
    childComponent.addItem(sdk)
  }

  private fun addSdkItemOnTop(sdk: Sdk) {
    childComponent.insertItemAt(sdk, 0)
  }

  fun setBusy(busy: Boolean) {
    if (busy) {
      childComponent.isEditable = true
      childComponent.editor = busyEditor
      (busyEditor.editorComponent as ExtendableTextField).addExtension(busyIconExtension)
    }
    else {
      (busyEditor.editorComponent as ExtendableTextField).removeExtension(busyIconExtension)
      childComponent.isEditable = false
    }
    repaint()
  }
}

/**
 * Kept for source compatibility with previous platform versions where the combobox could contain
 * a "new sdk" item. This reimplementation never adds such an item.
 */
class NewPySdkComboBoxItem

/**
 * Copy-pasted from intellij sources.
 * In 261 marked as deprecated to be removed, removed in 262.
 * TODO: rewrite
 */
fun addInterpretersAsync(
  sdkComboBox: PySdkPathChoosingComboBox,
  sdkObtainer: () -> List<Sdk>,
  onAdded: (List<Sdk>) -> Unit,
) {
  ApplicationManager.getApplication().executeOnPooledThread {
    val executor = AppUIExecutor.onUiThread(ModalityState.any())
    executor.execute { sdkComboBox.setBusy(true) }
    var sdks = emptyList<Sdk>()
    try {
      sdks = sdkObtainer()
    }
    finally {
      executor.execute {
        sdkComboBox.setBusy(false)
        sdkComboBox.removeAllItems()
        sdks.forEach(sdkComboBox::addSdkItem)
        onAdded(sdks)
      }
    }
  }
}

/**
 * Keeps [NewPySdkComboBoxItem] if it is present in the combobox.
 */
private fun PySdkPathChoosingComboBox.removeAllItems() {
  if (childComponent.itemCount > 0 && childComponent.getItemAt(0) is NewPySdkComboBoxItem) {
    while (childComponent.itemCount > 1) {
      childComponent.removeItemAt(1)
    }
  }
  else {
    childComponent.removeAllItems()
  }
}
