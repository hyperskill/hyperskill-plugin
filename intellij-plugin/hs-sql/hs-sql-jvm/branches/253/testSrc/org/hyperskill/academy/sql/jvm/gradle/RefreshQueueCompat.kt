package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl

/**
 * Since 2025.3 `RefreshQueueImpl` is a Kotlin class and `isRefreshInProgress` is read as a property. On 2025.2 it is
 * a Java class with a plain static `isRefreshInProgress()` method. No single syntax compiles on both.
 *
 * BACKCOMPAT: 252 -- drop the branch copies and inline the property read once 2025.2 support is dropped.
 */
internal fun isVfsRefreshInProgress(): Boolean = RefreshQueueImpl.isRefreshInProgress
