package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.openapi.vfs.newvfs.RefreshQueueImpl

/**
 * On 2025.2 `RefreshQueueImpl` is a Java class and `isRefreshInProgress()` is a plain static method. Since 2025.3 it
 * is a Kotlin class whose companion exposes the very same JVM method as a `@JvmStatic` property, which Kotlin can
 * only read as `RefreshQueueImpl.isRefreshInProgress`. No single syntax compiles on both.
 *
 * BACKCOMPAT: 252 -- drop the branch copies and inline the property read once 2025.2 support is dropped.
 */
internal fun isVfsRefreshInProgress(): Boolean = RefreshQueueImpl.isRefreshInProgress()
