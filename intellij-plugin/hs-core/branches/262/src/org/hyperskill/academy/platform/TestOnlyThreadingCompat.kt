package org.hyperskill.academy.platform

import com.intellij.openapi.application.impl.TestOnlyThreading

/**
 * Since 2026.1 `releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack` takes a
 * `java.lang.Runnable` and returns nothing. On 2025.3 it takes a Kotlin `() -> T`, and on 2025.2 the class does not
 * exist at all -- hence one copy of this shim per branch.
 */
fun runWithoutWriteIntentLock(action: () -> Unit) {
  TestOnlyThreading.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack(Runnable { action() })
}
