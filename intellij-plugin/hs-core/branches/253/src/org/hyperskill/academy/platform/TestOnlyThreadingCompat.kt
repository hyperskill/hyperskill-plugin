package org.hyperskill.academy.platform

import com.intellij.openapi.application.impl.TestOnlyThreading

/**
 * On 2025.3 `releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack` is a Kotlin function
 * taking `() -> T` and returning its result. Since 2026.1 it takes a `java.lang.Runnable` and returns nothing,
 * and on 2025.2 the class does not exist at all -- hence one copy of this shim per branch.
 */
fun runWithoutWriteIntentLock(action: () -> Unit) {
  TestOnlyThreading.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack(action)
}
