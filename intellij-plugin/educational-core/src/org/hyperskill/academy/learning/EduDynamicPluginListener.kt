package org.hyperskill.academy.learning

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.DynamicPluginVetoer
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.extensions.PluginId

class EduDynamicPluginListener : DynamicPluginListener, DynamicPluginVetoer {

  override fun vetoPluginUnload(pluginDescriptor: IdeaPluginDescriptor): String? {
    if (pluginDescriptor.pluginId == PluginId.getId(EduNames.PLUGIN_ID)) {
      return "Hyperskill Academy plugin unloading is not supported yet"
    }
    return null
  }
}
