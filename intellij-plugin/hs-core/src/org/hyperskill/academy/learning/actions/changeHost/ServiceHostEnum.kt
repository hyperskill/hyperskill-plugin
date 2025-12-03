package org.hyperskill.academy.learning.actions.changeHost

import com.intellij.openapi.util.NlsContexts.ListItem
import org.jetbrains.annotations.NonNls

interface ServiceHostEnum {
  @get:NonNls
  val url: String
  fun visibleName(): @ListItem String
}
