package org.hyperskill.academy.learning

import com.intellij.ide.plugins.IdeaPluginDescriptorImpl
import com.intellij.ide.plugins.PluginMainDescriptor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

internal fun collectFromModules(
  pluginDescriptor: IdeaPluginDescriptorImpl,
  collect: (moduleDescriptor: IdeaPluginDescriptorImpl) -> Unit
) {
  // In 2025.3, contentModules is only available on PluginMainDescriptor
  if (pluginDescriptor is PluginMainDescriptor) {
    for (module in pluginDescriptor.contentModules) {
      (module as? IdeaPluginDescriptorImpl)?.let { collect(it) }
    }
  }
}

// In 2025.3, ActionUtil.updateAction was removed. Use action.update(e) directly.
internal fun updateAction(action: AnAction, e: AnActionEvent) {
  action.update(e)
}
