package org.hyperskill.academy.cpp

import com.intellij.codeInsight.CodeInsightSettings

/**
 * Since 262 CLion has only the Nova engine, which turns on `AUTO_POPUP_JAVADOC_INFO` when the first project opens.
 * The platform then reports damaged code insight settings for whichever test happens to run first,
 * so tests revert it in `tearDown` before that check. With the classic engine, it's a no-op.
 */
internal fun revertNovaCodeInsightSettings() {
  CodeInsightSettings.getInstance().AUTO_POPUP_JAVADOC_INFO = CodeInsightSettings().AUTO_POPUP_JAVADOC_INFO
}
