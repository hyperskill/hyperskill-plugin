package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.database.view.DatabaseViewPanel
import com.intellij.ui.treeStructure.Tree

/**
 * In 262 `DatabaseViewPanel.getTree()` was replaced with `getUnderlyingDatabaseViewTree()`.
 *
 * BACKCOMPAT: 261 -- drop the branch copies and use `getUnderlyingDatabaseViewTree()` once 2026.1 support is dropped.
 */
internal val DatabaseViewPanel.databaseTree: Tree
  get() = getUnderlyingDatabaseViewTree()
