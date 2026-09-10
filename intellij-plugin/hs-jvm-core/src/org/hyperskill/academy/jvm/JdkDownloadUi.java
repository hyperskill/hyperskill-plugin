package org.hyperskill.academy.jvm;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.SdkModel;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.projectRoots.impl.jdkDownloader.JdkItem;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownload;
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Opens the IDE's own {@code Download JDK} dialog with the version a course requires already chosen, so that the
 * vendor and the install directory are the only things left to pick.
 *
 * Written in Java on purpose. The predicate parameter of the seven-argument {@code showDownloadUI} is
 * {@code Predicate<Object>} on 2025.2 and 2025.3 and {@code Predicate<JdkItem>} on 2026.1 and 2026.2. The erasure is
 * the same, so one raw {@code Predicate} compiles against every supported platform, while no single Kotlin spelling
 * does.
 */
public final class JdkDownloadUi {

  private JdkDownloadUi() {
  }

  /** Whether the IDE is able to download a JDK of [sdkType] at all. */
  public static boolean isAvailable(@NotNull SdkTypeId sdkType) {
    return findDownload(sdkType) != null;
  }

  /**
   * Shows the dialog, pinned to {@code featureVersion}, and hands the resulting task to {@code onTaskReady}.
   *
   * @return {@code false} when the IDE offers no downloader for {@code sdkType} and nothing was shown
   */
  @SuppressWarnings({"rawtypes", "unchecked"})
  public static boolean show(@NotNull SdkTypeId sdkType,
                             @NotNull SdkModel sdkModel,
                             @NotNull JComponent parentComponent,
                             @Nullable Project project,
                             int featureVersion,
                             @NotNull Consumer<? super SdkDownloadTask> onTaskReady) {
    SdkDownload download = findDownload(sdkType);
    if (download == null) return false;

    Predicate versionFilter = item -> item instanceof JdkItem && ((JdkItem)item).getJdkMajorVersion() == featureVersion;
    download.showDownloadUI(sdkType, sdkModel, parentComponent, project, null, versionFilter, onTaskReady);
    return true;
  }

  private static @Nullable SdkDownload findDownload(@NotNull SdkTypeId sdkType) {
    for (SdkDownload candidate : SdkDownload.EP_NAME.getExtensionList()) {
      if (candidate.supportsDownload(sdkType)) return candidate;
    }
    return null;
  }
}
