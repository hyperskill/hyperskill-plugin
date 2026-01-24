package org.hyperskill.academy.learning.framework.storage

import org.junit.After
import org.junit.Assert.assertEquals
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

  @Test
  fun `test save and get snapshot`() {
    val state = mapOf("test.txt" to "content")
    val id = storage.saveSnapshot(-1, state)
    val restored = storage.getSnapshot(id)
    assertEquals(state, restored)
  }

  @Test
  fun `test save and get snapshot with multiple files`() {
    val state = mapOf("file1.txt" to "content1", "file2.txt" to "content2")
    val id = storage.saveSnapshot(-1, state)
    val restored = storage.getSnapshot(id)
    assertEquals(state, restored)
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
    val state1 = mapOf("a.txt" to content)
    val state2 = mapOf("b.txt" to content)

    storage.saveSnapshot(-1, state1)
    storage.saveSnapshot(-1, state2)

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
    val state1 = mapOf("file.txt" to "content1")
    val id = storage.saveSnapshot(-1, state1)
    val commitHash1 = storage.resolveRef(id)!!

    val state2 = mapOf("file.txt" to "content2")
    storage.saveSnapshot(id, state2)
    val commitHash2 = storage.resolveRef(id)!!

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
    val state1 = mapOf("file.txt" to "content1")
    val id1 = storage.saveSnapshot(-1, state1)
    val commitHash1 = storage.resolveRef(id1)!!

    val state2 = mapOf("file.txt" to "content2")
    // Save state2 as a NEW ref (id2), but with id1's commit as parent
    val id2 = storage.saveSnapshot(-1, state2, id1)
    val commitHash2 = storage.resolveRef(id2)!!

    val commit2 = storage.getCommit(commitHash2)
    assertEquals(listOf(commitHash1), commit2.parentHashes)

    // Verify refs are separate pointers to shared/linked commit history
    assertEquals(commitHash2, storage.resolveRef(id2))
    assertEquals(commitHash1, storage.resolveRef(id1))
  }

  @Test
  fun `test HEAD not set initially`() {
    assertEquals(-1, storage.getHead())
    assertEquals(null, storage.getHeadCommit())
    assertEquals(null, storage.getHeadSnapshot())
  }

  @Test
  fun `test HEAD set and get`() {
    val state = mapOf("file.txt" to "content")
    val refId = storage.saveSnapshot(-1, state)

    storage.setHead(refId)

    assertEquals(refId, storage.getHead())
    assertEquals(storage.resolveRef(refId), storage.getHeadCommit())
    assertEquals(state, storage.getHeadSnapshot())
  }

  @Test
  fun `test HEAD update on navigation`() {
    val state1 = mapOf("file.txt" to "content1")
    val state2 = mapOf("file.txt" to "content2")

    val ref1 = storage.saveSnapshot(-1, state1)
    val ref2 = storage.saveSnapshot(-1, state2, ref1)

    // Simulate navigation: HEAD points to current stage
    storage.setHead(ref1)
    assertEquals(state1, storage.getHeadSnapshot())

    storage.setHead(ref2)
    assertEquals(state2, storage.getHeadSnapshot())
  }

  @Test
  fun `test HEAD clear`() {
    val state = mapOf("file.txt" to "content")
    val refId = storage.saveSnapshot(-1, state)
    storage.setHead(refId)
    assertEquals(refId, storage.getHead())

    storage.setHead(-1)  // Clear HEAD
    assertEquals(-1, storage.getHead())
    assertEquals(null, storage.getHeadSnapshot())
  }
}
