package com.jetbrains.python.sdk;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import kotlin.ResultKt;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Isolates access to Python 262's Kotlin-internal installable-SDK API.
 *
 * <p>The declarations are public in JVM bytecode, but Kotlin intentionally hides them from other
 * modules. Keeping the bridge in this platform-specific source set avoids reflection throughout
 * the course creation flow and gives the rest of the plugin a stable SDK-shaped model.</p>
 */
public final class PythonSdkInstallBridge {
  private PythonSdkInstallBridge() {
  }

  public static final class Suggestion {
    private final @NotNull PySdkToInstall delegate;
    private final @NotNull String name;
    private final @NotNull String version;

    private Suggestion(@NotNull PySdkToInstall delegate) {
      this.delegate = delegate;
      name = delegate.getName();
      version = delegate.getInstallation().getRelease().getVersion();
    }

    public @NotNull String getName() {
      return name;
    }

    public @NotNull String getVersion() {
      return version;
    }
  }

  public static @NotNull List<Suggestion> getSuggestions() {
    return PySdkToInstallKt.getSdksToInstall().stream().map(Suggestion::new).toList();
  }

  public static @NotNull Sdk install(
    @NotNull Suggestion suggestion,
    @Nullable Module module,
    @NotNull Function0<? extends List<? extends Sdk>> existingSdks
  ) throws Throwable {
    Method installMethod = findInstallMethod();
    final Object result;
    try {
      result = installMethod.invoke(suggestion.delegate, module, existingSdks);
    }
    catch (InvocationTargetException e) {
      throw e.getCause();
    }
    ResultKt.throwOnFailure(result);
    return (Sdk)result;
  }

  private static @NotNull Method findInstallMethod() {
    for (Method method : PySdkToInstall.class.getMethods()) {
      if (method.getName().startsWith("install-") && method.getParameterCount() == 2) {
        return method;
      }
    }
    throw new IllegalStateException("Python SDK install method is not available");
  }
}
