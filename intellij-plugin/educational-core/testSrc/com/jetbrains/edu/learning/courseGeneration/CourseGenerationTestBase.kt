package org.hyperskill.academy.learning.courseGeneration

import com.intellij.diagnostic.dumpCoroutines
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.observation.Observation
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import kotlinx.coroutines.*
import kotlinx.coroutines.future.asCompletableFuture
import org.hyperskill.academy.learning.EduHeavyTestCase
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.assertContentsEqual
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.FileContents
import org.hyperskill.academy.learning.courseFormat.InMemoryBinaryContents
import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.ext.disambiguateContents
import org.hyperskill.academy.learning.newproject.EduProjectSettings
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import kotlin.time.Duration.Companion.minutes

@Suppress("UnstableApiUsage")
abstract class CourseGenerationTestBase<Settings : EduProjectSettings> : EduHeavyTestCase() {

  abstract val defaultSettings: Settings

  override fun tearDown() = invokeAndWaitIfNeeded {
    super.tearDown()
  }

  protected val rootDir: VirtualFile by lazy { tempDir.createVirtualDir() }

  protected fun findFile(path: String): VirtualFile = rootDir.findFileByRelativePath(path) ?: error("Can't find $path")

  protected open fun createCourseStructure(
    course: Course,
    metadata: Map<String, String> = emptyMap(),
    waitForProjectConfiguration: Boolean = true
  ) {
    val configurator = course.configurator ?: error("Failed to find `EduConfigurator` for `${course.name}` course")
    val generator = configurator.courseBuilder.getCourseProjectGenerator(course)
                    ?: error("The provided builder returned null as the course project generator")
    val project = invokeAndWaitIfNeeded {
      generator.doCreateCourseProject(rootDir.path, defaultSettings, metadata) ?: error("Cannot create project")
    }

    if (waitForProjectConfiguration) {
      waitForCourseConfiguration(project)
      IndexingTestUtil.waitUntilIndexesAreReady(project)
    }
    TaskToolWindowView.getInstance(project).currentTask = project.getCurrentTask()
    runInEdtAndWait {
      myProject = project
    }
  }

  /**
   * It intentionally does nothing to avoid project creation in [setUp].
   *
   * If you need to create course project, use [createCourseStructure]
   */
  override fun setUpProject() {}

  protected fun assertListOfAdditionalFiles(course: Course, vararg files: Pair<String, Any?>) {
    val filesMap = mapOf(*files)
    assertEquals("Unexpected list of additional files", filesMap.keys, course.additionalFiles.map { it.name }.toSet())

    for (actualFile in course.additionalFiles) {
      val path = actualFile.name

      if (!filesMap.containsKey(path)) fail("Unexpected additional file $path")

      val expectedContents = when (val value = filesMap[path]) {
        is String -> InMemoryTextualContents(value)
        is ByteArray -> InMemoryBinaryContents(value)
        is FileContents -> value
        null -> null
        else -> error("value should be either a String or a ByteArray or a FileContents, got ${value::class.java} instead")
      }
      val actualContents = actualFile.contents.disambiguateContents(path)
      if (expectedContents != null) {
        assertContentsEqual(path, expectedContents, actualContents)
      }
    }
  }

  protected fun waitForCourseConfiguration(project: Project) {
    @OptIn(DelicateCoroutinesApi::class)
    val job = GlobalScope.launch { Observation.awaitConfiguration(project) }
    if (ApplicationManager.getApplication().isDispatchThread) {
      PlatformTestUtil.waitForFuture(job.asCompletableFuture())
    }
    else {
      runBlockingMaybeCancellable {
        try {
          // The same timeout as `PlatformTestUtil.MAX_WAIT_TIME`
          withTimeout(2.minutes) {
            job.join()
          }
        }
        catch (e: TimeoutCancellationException) {
          throw RuntimeException("Cannot wait for course configuration to complete\n${dumpCoroutines()}", e)
        }
      }
    }
  }
}
