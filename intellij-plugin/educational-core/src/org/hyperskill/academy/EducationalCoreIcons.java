package org.hyperskill.academy;

import com.intellij.openapi.util.IconLoader;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class EducationalCoreIcons {
  public static final Icon DOT = load("/icons/org/hyperskill/academy/learning/dot.svg"); // 3x3

  /**
   * Loads an icon using the default method.
   * This is currently the default way to load icons based on their path in the `resources` directory.
   * Note that there is also a method available for loading icons as rasterized images
   * using the {@link #loadRasterized(String)} method.
   * <p/>
   * However, the {@link #loadRasterized(String)} method has some limitations because it uses platform API that is not fully adjusted
   * for our needs and may not perfectly fit our use case, but it remains a necessary option for now.
   *
   * @param path the path to the icon in the `resources` directory.
   * @return the loaded {@link Icon} object.
   */
  private static @NotNull Icon load(@NotNull String path) {
    return IconLoader.getIcon(path, EducationalCoreIcons.class);
  }

  /**
   * Loads an icon in a modern and efficient manner as a rasterized image.
   * Currently, this method is used exclusively for loading tool window icons.
   * <p>
   * Note: The last two arguments passed to {@link IconManager#loadRasterizedIcon} are `cacheKey` and `flags`.
   * Both are set to 0, which is acceptable; however, we should consider using a different API in the future
   * that does not apply caching, which is currently unavailable
   *
   * @param path the path to the icon in the resources directory, without a leading slash.
   *             The path must be relative to the classpath root.
   * @return the loaded {@link Icon} object.
   * @throws IllegalArgumentException if the provided path starts with a leading slash.
   */
  private static @NotNull Icon loadRasterized(@NotNull String path) {
    if (path.startsWith("/")) {
      throw new IllegalArgumentException("Path must be specified without a leading slash");
    }

    return IconManager.getInstance().loadRasterizedIcon(path, EducationalCoreIcons.class.getClassLoader(), 0, 0);
  }

  /**
   * Utility class that provides icons for various actions
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Actions {
    public static final Icon ApplyCode = load("/icons/org/hyperskill/academy/learning/applyCode.svg");
    public static final Icon CommentTask = load("/icons/org/hyperskill/academy/learning/commentTask.svg");
    public static final Icon EduCourse = load("/icons/org/hyperskill/academy/eduCourseAction.svg");
    public static final Icon IgnoreSyncFile = load("/icons/org/hyperskill/academy/actions/syncFilesIgnore.svg");
    public static final Icon RateCourse = load("/icons/org/hyperskill/academy/learning/rateCourse.svg");
    public static final Icon ResetTask = load("/icons/org/hyperskill/academy/learning/resetTask.svg");
    public static final Icon SyncChanges = load("/icons/org/hyperskill/academy/actions/syncFiles.svg");
  }

  /**
   * Utility class that provides icons for the check panel.
   */
  public static final class CheckPanel {
    public static final Icon CheckDetailsToolWindow = loadRasterized("icons/org/hyperskill/academy/learning/checkDetailsToolWindow.svg");
    public static final Icon ResultCorrect = load("/icons/org/hyperskill/academy/learning/resultCorrect.svg");
    public static final Icon ResultIncorrect = load("/icons/org/hyperskill/academy/learning/resultIncorrect.svg");
  }

  /**
   * Utility class that provides icons for the course creator.
   */
  public static final class CourseCreator {
    public static final Icon GuidedProject = load("/icons/org/hyperskill/academy/courseCreator/guidedProject.svg");
    public static final Icon GuidedProjectSelected = load("/icons/org/hyperskill/academy/courseCreator/guidedProjectSelected.svg");
    public static final Icon SimpleLesson = load("/icons/org/hyperskill/academy/courseCreator/simpleLesson.svg");
    public static final Icon SimpleLessonSelected = load("/icons/org/hyperskill/academy/courseCreator/simpleLessonSelected.svg");
    public static final Icon NewLesson = load("/icons/org/hyperskill/academy/courseCreator/addLesson.svg");
    public static final Icon NewTask = load("/icons/org/hyperskill/academy/courseCreator/addTask.svg");
  }

  /**
   * Utility class that provides icons for various components in the Course View
   */
  public static final class CourseView {
    public static final Icon CourseTree = load("/icons/org/hyperskill/academy/eduCourseTree.svg");
    public static final Icon IdeTask = load("/icons/org/hyperskill/academy/eduTaskIdeDefault.svg");
    public static final Icon IdeTaskSolved = load("/icons/org/hyperskill/academy/eduTaskIdeDone.svg");
    public static final Icon Lesson = load("/icons/org/hyperskill/academy/eduLessonDefault.svg");
    public static final Icon LessonSolved = load("/icons/org/hyperskill/academy/eduLessonDone.svg");
    public static final Icon Section = load("/icons/org/hyperskill/academy/eduSectionDefault.svg");
    public static final Icon SectionSolved = load("/icons/org/hyperskill/academy/eduSectionDone.svg");
    public static final Icon SyncFilesModInfo = load("/icons/org/hyperskill/academy/syncFilesModInfo.svg");
    public static final Icon SyncFilesModWarning = load("/icons/org/hyperskill/academy/syncFilesModWarning.svg");
    public static final Icon Task = load("/icons/org/hyperskill/academy/eduTaskDefault.svg");
    public static final Icon TaskFailed = load("/icons/org/hyperskill/academy/eduTaskFailed.svg");
    public static final Icon TaskSolved = load("/icons/org/hyperskill/academy/eduTaskDone.svg");
    public static final Icon TheoryTask = load("/icons/org/hyperskill/academy/eduTaskTheoryDefault.svg");
    public static final Icon TheoryTaskSolved = load("/icons/org/hyperskill/academy/eduTaskTheoryDone.svg");
    public static final Icon UsersNumber = load("/icons/org/hyperskill/academy/usersNumber.svg");
  }

  /**
   * Utility class that provides icons for various programming languages
   *
   * <p>All icons are 16x16</p>
   */
  public static final class Language {
    public static final Icon Android = load("/icons/org/hyperskill/academy/learning/AndroidLogo.svg");
    public static final Icon Cpp = load("/icons/org/hyperskill/academy/learning/CAndC++Logo.svg");
    public static final Icon CSharp = load("/icons/org/hyperskill/academy/learning/CSharpLogo.svg");
    public static final Icon Go = load("/icons/org/hyperskill/academy/learning/GoLogo.svg");
    public static final Icon Java = load("/icons/org/hyperskill/academy/learning/JavaLogo.svg");
    public static final Icon JavaScript = load("/icons/org/hyperskill/academy/learning/JavaScriptLogo.svg");
    public static final Icon Kotlin = load("/icons/org/hyperskill/academy/learning/KotlinLogo.svg");
    public static final Icon Php = load("/icons/org/hyperskill/academy/learning/PhpLogo.svg");
    public static final Icon Python = load("/icons/org/hyperskill/academy/learning/PythonLogo.svg");
    public static final Icon Rust = load("/icons/org/hyperskill/academy/learning/RustLogo.svg");
    public static final Icon Scala = load("/icons/org/hyperskill/academy/learning/ScalaLogo.svg");
    public static final Icon Shell = load("/icons/org/hyperskill/academy/learning/ShellLogo.svg");
  }

  /**
   * Utility class that provides icons for various educational platforms
   *
   * <p>All child icons are 16x16</p>
   */
  public static final class Platform {
    public static final Icon HyperskillAcademy = load("/icons/org/hyperskill/academy/learning/JB_academy.svg");

    /**
     * Class that provides tab icons for different platforms
     *
     * <p>Tab icons are 24x24 and are supposed to be used only in implementations of
     * {@code org.hyperskill.academy.learning.newproject.ui.CoursesPlatformProviderFactory}</p>
     */
    public static final class Tab {
      public static final Icon HyperskillAcademyTab = load("/icons/org/hyperskill/academy/learning/JB_academy_course_tab.svg");
    }
  }

  /**
   * Utility class that provides icons for task submissions
   *
   * <p>All icons are 11x11</p>
   */
  public static final class Submission {
    public static final Icon TaskFailed = load("/icons/org/hyperskill/academy/submission/taskFailed@2x.png");
    public static final Icon TaskFailedHighContrast = load("/icons/org/hyperskill/academy/submission/taskFailedHighContrast@2x.png");
    public static final Icon TaskSolved = load("/icons/org/hyperskill/academy/submission/taskSolved@2x.png");
    public static final Icon TaskSolvedHighContrast = load("/icons/org/hyperskill/academy/submission/taskSolvedHighContrast@2x.png");
  }

  /**
   * Utility class that provides icons for the task tool window
   */
  public static final class TaskToolWindow {
    public static final Icon Clock = load("/icons/org/hyperskill/academy/learning/clock.svg");
    public static final Icon CourseToolWindow = loadRasterized("icons/org/hyperskill/academy/eduCourseTask.svg");
    public static final Icon MoveDown = load("/icons/org/hyperskill/academy/learning/moveDown.svg");
    public static final Icon MoveUp = load("/icons/org/hyperskill/academy/learning/moveUp.svg");
    public static final Icon NavigationMapTheoryTask = load("/icons/org/hyperskill/academy/eduNavigationMapTheoryTask.svg");
  }
}
