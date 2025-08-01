package org.hyperskill.academy.learning.newproject.ui

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.util.NlsContexts.LinkLabel
import java.util.function.Supplier

/**
 * It is used to provide sentence capitalized action text to show it in [BrowseCoursesDialog] dialog toolbar
 */
class ToolbarActionWrapper(@Suppress("UnstableApiUsage") @LinkLabel val text: Supplier<String>, val action: AnAction)