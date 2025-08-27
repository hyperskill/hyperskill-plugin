package org.hyperskill.academy.learning.yaml

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.isRemoteConfigFile
import java.util.function.Function
import javax.swing.JComponent

class GeneratedRemoteInfoNotificationProvider(val project: Project) : EditorNotificationProvider, DumbAware {

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    return if (isRemoteConfigFile(file)) {
      Function { EditorNotificationPanel().text(EduCoreBundle.message("yaml.remote.config.notification")) }
    }
    else {
      null
    }
  }
}
