package org.hyperskill.academy.ai.debugger.core.ui;

import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class AIDebuggerIcons {
  public static final Icon Bug = load("/icons/bug.svg");

  /**
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@NotNull String path) {
    return IconLoader.getIcon(path, AIDebuggerIcons.class);
  }
}