package org.hyperskill.academy.platform;

import com.intellij.ide.impl.OpenProjectTask;
import com.intellij.ide.impl.OpenProjectTaskKt;
import com.intellij.openapi.project.Project;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import org.jetbrains.annotations.Nullable;

/**
 * Builds {@link OpenProjectTask} in a way that survives platform updates within a single branch.
 *
 * <p>{@code OpenProjectTask} is a Kotlin data class, so both its constructor and its {@code copy} change signature
 * whenever the platform adds a property ({@code opensFileAfterProjectOpen} was added in 262.9437). The
 * {@code OpenProjectTask { ... }} DSL avoids naming those signatures in source, but it is an {@code inline} function,
 * so a Kotlin caller still gets the unstable constructor baked into its own bytecode and fails with
 * {@code NoSuchMethodError} on any other build.
 *
 * <p>This class is deliberately written in Java: Java cannot inline a Kotlin {@code inline} function, so the call
 * compiles to a plain {@code OpenProjectTaskKt.OpenProjectTask(Function1)} invocation. That method and the
 * {@code OpenProjectTaskBuilder} setters used below have identical signatures on every platform this plugin supports
 * (252 through 262.9437), so the resulting bytecode is version independent.
 */
public final class OpenProjectTaskFactory {

  private OpenProjectTaskFactory() {
  }

  @SuppressWarnings("unchecked")
  public static OpenProjectTask buildForOpen(
    boolean forceOpenInNewFrame,
    boolean isNewProject,
    boolean isProjectCreatedWithWizard,
    boolean runConfigurators,
    @Nullable String projectName,
    @Nullable Project projectToClose,
    @Nullable Function1<Project, Unit> beforeInit,
    @Nullable Object preparedToOpen
  ) {
    return OpenProjectTaskKt.OpenProjectTask(builder -> {
      builder.setForceOpenInNewFrame(forceOpenInNewFrame);
      builder.setNewProject(isNewProject);
      builder.setProjectCreatedWithWizard(isProjectCreatedWithWizard);
      builder.setRunConfigurators(runConfigurators);
      builder.setProjectName(projectName);
      builder.setProjectToClose(projectToClose);
      if (beforeInit != null) {
        builder.setBeforeInit(beforeInit);
      }
      if (preparedToOpen != null) {
        // `preparedToOpen` is a `suspend (Module) -> Unit` created on the Kotlin side; its JVM representation is
        // Function2<Module, Continuation<? super Unit>, Object>. It is passed as Object so that Java never has to
        // spell out the continuation type.
        builder.setPreparedToOpen((Function2<com.intellij.openapi.module.Module, kotlin.coroutines.Continuation<? super Unit>, Object>) preparedToOpen);
      }
      return Unit.INSTANCE;
    });
  }
}
