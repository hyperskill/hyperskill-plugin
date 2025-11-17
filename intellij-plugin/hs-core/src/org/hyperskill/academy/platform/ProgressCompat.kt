@file:Suppress("DEPRECATION")
package org.hyperskill.academy.platform

import com.intellij.openapi.progress.blockingContext

/**
 * Small compatibility shim for deprecated `blockingContext`.
 *
 * On IDE 2024.2+ (253+), blocking context is installed implicitly and the function
 * may be removed; on older lines (≤252) the function exists and should be used.
 *
 * This wrapper attempts to call the legacy function and falls back to a direct invocation
 * of the action when the symbol is missing at runtime.
 */
object ProgressCompat {
  suspend inline fun <T> withBlockingIfNeeded(crossinline action: () -> T): T {
    return try {
      blockingContext { action() }
    }
    catch (_: NoSuchMethodError) {
      action()
    }
  }
}