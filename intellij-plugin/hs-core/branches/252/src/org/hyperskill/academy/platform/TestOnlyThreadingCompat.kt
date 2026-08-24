package org.hyperskill.academy.platform

/**
 * `com.intellij.openapi.application.impl.TestOnlyThreading` only exists since 2025.3.
 * On 2025.2 the caller does not hold the write intent lock while dispatching invocation events, so the action runs
 * as is -- exactly what this code did before the lock dance was introduced in `Release fixes (#54)`.
 *
 * BACKCOMPAT: 252 -- drop this branch copy once 2025.2 is no longer supported.
 */
fun runWithoutWriteIntentLock(action: () -> Unit) {
  action()
}
