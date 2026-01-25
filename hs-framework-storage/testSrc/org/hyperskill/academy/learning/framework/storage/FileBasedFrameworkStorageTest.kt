package org.hyperskill.academy.learning.framework.storage

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class FileBasedFrameworkStorageTest {
  private lateinit var storageDir: Path
  private lateinit var storage: FileBasedFrameworkStorage

  @Before
  fun setUp() {
    storageDir = Files.createTempDirectory("framework-storage-test")
    storage = FileBasedFrameworkStorage(storageDir)
  }

  @After
  fun tearDown() {
    if (::storage.isInitialized) {
      storage.close()
    }
    if (::storageDir.isInitialized) {
      deleteRecursively(storageDir)
    }
  }

  private fun deleteRecursively(path: Path) {
    if (Files.exists(path)) {
      Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .forEach { Files.deleteIfExists(it) }
    }
  }

  // Helper to convert simple string map to FileEntry map
  private fun state(vararg pairs: Pair<String, String>): Map<String, FileEntry> =
    pairs.associate { (path, content) -> path to FileEntry(content) }

  // Helper to extract content from FileEntry map for comparison
  private fun Map<String, FileEntry>.toContentMap(): Map<String, String> =
    mapValues { it.value.content }

  @Test
  fun `test save and get snapshot`() {
    val state = state("test.txt" to "content")
    storage.saveSnapshot("stage_1", state)
    val restored = storage.getSnapshot("stage_1")
    assertEquals(state.toContentMap(), restored.toContentMap())
  }

  @Test
  fun `test save and get snapshot with multiple files`() {
    val state = state("file1.txt" to "content1", "file2.txt" to "content2")
    storage.saveSnapshot("stage_1", state)
    val restored = storage.getSnapshot("stage_1")
    assertEquals(state.toContentMap(), restored.toContentMap())
  }

  @Test
  fun `test calculate diff from snapshots`() {
    val initialState = mapOf("file1.txt" to "content1", "file2.txt" to "content2")
    val currentState = mapOf("file1.txt" to "modified", "file3.txt" to "new")

    val changes = SnapshotDiff.calculateChanges(initialState, currentState)

    // file1.txt: modified -> ChangeFile
    // file2.txt: removed -> RemoveFile
    // file3.txt: added -> AddFile
    assertEquals(3, changes.changes.size)

    val changeFile = changes.changes.filterIsInstance<Change.ChangeFile>().single()
    assertEquals("file1.txt", changeFile.path)
    assertEquals("modified", changeFile.text)

    val removeFile = changes.changes.filterIsInstance<Change.RemoveFile>().single()
    assertEquals("file2.txt", removeFile.path)

    val addFile = changes.changes.filterIsInstance<Change.AddFile>().single()
    assertEquals("file3.txt", addFile.path)
    assertEquals("new", addFile.text)
  }

  @Test
  fun `test apply changes to state`() {
    val initialState = mapOf("file1.txt" to "content1", "file2.txt" to "content2")
    val changes = UserChanges(listOf(
      Change.ChangeFile("file1.txt", "modified"),
      Change.RemoveFile("file2.txt"),
      Change.AddFile("file3.txt", "new")
    ))

    val result = SnapshotDiff.applyChanges(initialState, changes)

    val expected = mapOf("file1.txt" to "modified", "file3.txt" to "new")
    assertEquals(expected, result)
  }

  @Test
  fun `test deduplication`() {
    val content = "shared content"
    val state1 = state("a.txt" to content)
    val state2 = state("b.txt" to content)

    storage.saveSnapshot("stage_1", state1)
    storage.saveSnapshot("stage_2", state2)

    // Count files in objects directory (excluding subdirectories)
    val objectsCount = Files.walk(storageDir.resolve("objects"))
      .filter { Files.isRegularFile(it) }
      .count()

    // state1: 1 blob, 1 snapshot, 1 commit = 3
    // state2: 0 new blobs, 1 snapshot, 1 commit = 2
    // total = 5
    assertEquals(5, objectsCount)
  }

  @Test
  fun `test commit history`() {
    val state1 = state("file.txt" to "content1")
    storage.saveSnapshot("stage_1", state1)
    val commitHash1 = storage.resolveRef("stage_1")!!

    val state2 = state("file.txt" to "content2")
    storage.saveSnapshot("stage_1", state2)  // Update same ref
    val commitHash2 = storage.resolveRef("stage_1")!!

    val commit2 = storage.getCommit(commitHash2)
    assertEquals(listOf(commitHash1), commit2.parentHashes)

    val commit1 = storage.getCommit(commitHash1)
    assertEquals(emptyList<String>(), commit1.parentHashes)
  }

  @Test
  fun `test collision resolution`() {
    val hash = "fake_hash"

    // Save first object with fake_hash
    val id1 = storage.saveObject(1, hash) { it.writeUTF("content1") }
    assertEquals(hash, id1)

    // Save second object with SAME fake_hash but DIFFERENT content
    val id2 = storage.saveObject(1, hash) { it.writeUTF("content2") }
    assertEquals("${hash}_1", id2)

    // Save third object with SAME fake_hash and SAME content as first
    val id3 = storage.saveObject(1, hash) { it.writeUTF("content1") }
    assertEquals(hash, id3)

    // Verify we can read both
    val read1 = storage.readObject(id1) { _, input -> input.readUTF() }
    val read2 = storage.readObject(id2) { _, input -> input.readUTF() }

    assertEquals("content1", read1)
    assertEquals("content2", read2)
  }

  @Test
  fun `test cross-ref history`() {
    val state1 = state("file.txt" to "content1")
    storage.saveSnapshot("stage_1", state1)
    val commitHash1 = storage.resolveRef("stage_1")!!

    val state2 = state("file.txt" to "content2")
    // Save state2 as a NEW ref (stage_2), but with stage_1's commit as parent
    storage.saveSnapshot("stage_2", state2, "stage_1")
    val commitHash2 = storage.resolveRef("stage_2")!!

    val commit2 = storage.getCommit(commitHash2)
    assertEquals(listOf(commitHash1), commit2.parentHashes)

    // Verify refs are separate pointers to shared/linked commit history
    assertEquals(commitHash2, storage.resolveRef("stage_2"))
    assertEquals(commitHash1, storage.resolveRef("stage_1"))
  }

  @Test
  fun `test HEAD not set initially`() {
    assertNull(storage.getHead())
    assertNull(storage.getHeadCommit())
    assertNull(storage.getHeadSnapshot())
  }

  @Test
  fun `test HEAD set and get`() {
    val state = state("file.txt" to "content")
    storage.saveSnapshot("stage_1", state)

    storage.setHead("stage_1")

    assertEquals("stage_1", storage.getHead())
    assertEquals(storage.resolveRef("stage_1"), storage.getHeadCommit())
    assertEquals(state.toContentMap(), storage.getHeadSnapshot()?.toContentMap())
  }

  @Test
  fun `test HEAD update on navigation`() {
    val state1 = state("file.txt" to "content1")
    val state2 = state("file.txt" to "content2")

    storage.saveSnapshot("stage_1", state1)
    storage.saveSnapshot("stage_2", state2, "stage_1")

    // Simulate navigation: HEAD points to current stage
    storage.setHead("stage_1")
    assertEquals(state1.toContentMap(), storage.getHeadSnapshot()?.toContentMap())

    storage.setHead("stage_2")
    assertEquals(state2.toContentMap(), storage.getHeadSnapshot()?.toContentMap())
  }

  @Test
  fun `test HEAD clear`() {
    val state = state("file.txt" to "content")
    storage.saveSnapshot("stage_1", state)
    storage.setHead("stage_1")
    assertEquals("stage_1", storage.getHead())

    storage.setHead(null)  // Clear HEAD
    assertNull(storage.getHead())
    assertNull(storage.getHeadSnapshot())
  }

  @Test
  fun `test hasRef`() {
    assertFalse(storage.hasRef("stage_1"))

    val state = state("file.txt" to "content")
    storage.saveSnapshot("stage_1", state)

    assertTrue(storage.hasRef("stage_1"))
    assertFalse(storage.hasRef("stage_2"))
  }

  @Test
  fun `test getAllRefNames`() {
    assertEquals(emptyList<String>(), storage.getAllRefNames())

    storage.saveSnapshot("stage_1", state("a.txt" to "a"))
    storage.saveSnapshot("stage_2", state("b.txt" to "b"))
    storage.saveSnapshot("step_123", state("c.txt" to "c"))

    val refs = storage.getAllRefNames()
    assertEquals(listOf("stage_1", "stage_2", "step_123"), refs)
  }

  @Test
  fun `test skip empty commit when snapshot is identical`() {
    val state = state("file.txt" to "content")

    // First save creates a commit
    val created1 = storage.saveSnapshot("stage_1", state)
    assertTrue(created1)
    val hash1 = storage.resolveRef("stage_1")!!

    // Second save with identical content should not create new commit
    val created2 = storage.saveSnapshot("stage_1", state)
    assertFalse(created2)
    val hash2 = storage.resolveRef("stage_1")!!

    // Hash should be the same (no new commit)
    assertEquals(hash1, hash2)
  }

  @Test
  fun `test create new ref with identical content as parent`() {
    val state = state("file.txt" to "content")

    // First save creates a commit for stage_1
    val created1 = storage.saveSnapshot("stage_1", state)
    assertTrue(created1)
    val hash1 = storage.resolveRef("stage_1")!!

    // Save identical content to a NEW ref (stage_2) with stage_1 as parent
    // This creates a new commit to preserve correct parent chain
    val created2 = storage.saveSnapshot("stage_2", state, "stage_1")
    assertTrue(created2) // New commit created for correct parent chain

    // Ref should exist
    assertTrue(storage.hasRef("stage_2"))
    val hash2 = storage.resolveRef("stage_2")!!

    // Different commits (to preserve parent chain)
    assertNotEquals(hash1, hash2)

    // But stage_2's commit should have stage_1's commit as parent
    val commit2 = storage.getCommit(hash2)
    assertEquals(listOf(hash1), commit2.parentHashes)

    // Verify both refs are listed
    val refs = storage.getAllRefNames()
    assertTrue(refs.contains("stage_1"))
    assertTrue(refs.contains("stage_2"))
  }

  @Test
  fun `test FileEntry with metadata`() {
    val entry = FileEntry.create(
      content = "test content",
      visible = false,
      editable = true,
      propagatable = false,
      highlightLevel = "NONE"
    )

    val state = mapOf("file.kt" to entry)
    storage.saveSnapshot("stage_1", state)

    val restored = storage.getSnapshot("stage_1")
    val restoredEntry = restored["file.kt"]!!

    assertEquals("test content", restoredEntry.content)
    assertEquals(false, restoredEntry.isVisible)
    assertEquals(true, restoredEntry.isEditable)
    assertEquals(false, restoredEntry.isPropagatable)
    assertEquals("NONE", restoredEntry.highlightLevel)
  }

  @Test
  fun `test FileEntry default metadata`() {
    val entry = FileEntry("content only")

    assertEquals("content only", entry.content)
    assertEquals(true, entry.isVisible)
    assertEquals(true, entry.isEditable)
    assertEquals(true, entry.isPropagatable)
    assertEquals("ALL_PROBLEMS", entry.highlightLevel)
  }
}
