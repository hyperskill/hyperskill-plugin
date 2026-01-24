package org.hyperskill.academy.learning.framework.impl.migration

import com.intellij.openapi.util.Disposer
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase
import org.hyperskill.academy.learning.framework.storage.Change
import org.hyperskill.academy.learning.framework.storage.UserChanges
import org.hyperskill.academy.learning.framework.impl.FrameworkLessonManagerImpl
import org.hyperskill.academy.learning.framework.impl.FrameworkStorage
import org.hyperskill.academy.learning.framework.impl.LegacyFrameworkStorage
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings
import org.junit.Test
import java.io.DataInputStream

class FrameworkStorageMigrationTest : CourseGenerationTestBase<EmptyProjectSettings>() {

  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  override fun setUpProject() {
    val course = course {}
    createCourseStructure(course)
  }

  @Test
  fun `test read legacy changes`() {
    val storagePath = FrameworkLessonManagerImpl.constructStoragePath(project)

    // Create legacy storage with some changes
    val legacyStorage = LegacyFrameworkStorage(storagePath)
    legacyStorage.version = 0
    val oldChanges = UserChanges0(listOf(Change.AddFile("foo/bar.txt", "FooBar")))
    val record = legacyStorage.createRecordWithData(oldChanges)
    Disposer.dispose(legacyStorage)

    // Create new storage wrapper - it should detect legacy storage exists
    val storage = FrameworkStorage(storagePath)
    Disposer.register(testRootDisposable, storage)

    // Verify we can read legacy changes
    assertTrue("Legacy storage should exist", storage.hasLegacyStorage())
    val legacyChanges = storage.getLegacyChanges(record)
    assertNotNull("Legacy changes should be readable", legacyChanges)
    assertEquals(oldChanges.changes, legacyChanges!!.changes)
  }

  @Test
  fun `test apply legacy changes on top of base state`() {
    val storagePath = FrameworkLessonManagerImpl.constructStoragePath(project)

    // Create legacy storage with some changes
    val legacyStorage = LegacyFrameworkStorage(storagePath)
    legacyStorage.version = 0
    val oldChanges = UserChanges0(listOf(
      Change.AddFile("user_created.txt", "User content"),
      Change.ChangeFile("existing.txt", "Modified content")
    ))
    val record = legacyStorage.createRecordWithData(oldChanges)
    Disposer.dispose(legacyStorage)

    // Create new storage wrapper
    val storage = FrameworkStorage(storagePath)
    Disposer.register(testRootDisposable, storage)

    // Base state from API (without user's local changes)
    val baseState = mapOf("existing.txt" to "Original content", "template.txt" to "Template")

    // Apply legacy changes on top of base state
    val newRefId = storage.applyLegacyChangesAndSave(record, baseState)

    // Verify the resulting snapshot contains merged state
    val mergedState = storage.getSnapshot(newRefId)
    assertEquals("Modified content", mergedState["existing.txt"])
    assertEquals("User content", mergedState["user_created.txt"])
    assertEquals("Template", mergedState["template.txt"])
  }

  @Test
  fun `test migrate from 0 to 1`() {
    val storagePath = FrameworkLessonManagerImpl.constructStoragePath(project)
    
    val legacyStorage = LegacyFrameworkStorage(storagePath)
    legacyStorage.version = 0
    val oldChanges = UserChanges0(listOf(Change.AddFile("foo/bar.txt", "FooBar")))
    val record = legacyStorage.createRecordWithData(oldChanges)
    
    legacyStorage.migrate(1)

    val newChanges = legacyStorage.readStream(record).use { inputStream -> 
      DataInputStream(inputStream).use { UserChanges1.read(it) } 
    }

    assertEquals(oldChanges.changes, newChanges.changes)
    assertEquals(-1, newChanges.timestamp)
    
    Disposer.dispose(legacyStorage)
  }
}
