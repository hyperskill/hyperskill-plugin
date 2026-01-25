package org.hyperskill.academy.learning.framework.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileEntryTest {

  @Test
  fun `default metadata values`() {
    val entry = FileEntry("content")
    assertTrue(entry.isVisible)
    assertTrue(entry.isEditable)
    assertTrue(entry.isPropagatable)
    assertEquals("ALL_PROBLEMS", entry.highlightLevel)
  }

  @Test
  fun `create with non-default values stores only non-defaults`() {
    val entry = FileEntry.create(
      content = "test",
      visible = false,
      editable = true,  // default
      propagatable = false
    )
    assertEquals(mapOf("visible" to false, "propagatable" to false), entry.metadata)
    assertFalse(entry.isVisible)
    assertTrue(entry.isEditable)
    assertFalse(entry.isPropagatable)
  }

  // ==================== isTestFilePath tests ====================

  @Test
  fun `isTestFilePath - file in test directory`() {
    assertTrue(FileEntry.isTestFilePath("test/helper.py", listOf("test")))
    assertTrue(FileEntry.isTestFilePath("test/subdir/helper.py", listOf("test")))
    assertTrue(FileEntry.isTestFilePath("tests/test_main.py", listOf("tests")))
  }

  @Test
  fun `isTestFilePath - file not in test directory`() {
    assertFalse(FileEntry.isTestFilePath("src/main.py", listOf("test")))
    assertFalse(FileEntry.isTestFilePath("main.py", listOf("test")))
  }

  @Test
  fun `isTestFilePath - tests_py pattern`() {
    assertTrue(FileEntry.isTestFilePath("tests.py", emptyList()))
    assertTrue(FileEntry.isTestFilePath("src/tests.py", emptyList()))
  }

  @Test
  fun `isTestFilePath - test_ prefix pattern`() {
    assertTrue(FileEntry.isTestFilePath("test_main.py", emptyList()))
    assertTrue(FileEntry.isTestFilePath("src/test_utils.py", emptyList()))
  }

  @Test
  fun `isTestFilePath - _test_py suffix pattern`() {
    assertTrue(FileEntry.isTestFilePath("main_test.py", emptyList()))
    assertTrue(FileEntry.isTestFilePath("utils_test.py", emptyList()))
  }

  @Test
  fun `isTestFilePath - Java test patterns`() {
    assertTrue(FileEntry.isTestFilePath("MainTest.java", emptyList()))
    assertTrue(FileEntry.isTestFilePath("src/test/MainTest.java", emptyList()))
    assertTrue(FileEntry.isTestFilePath("MainTests.java", emptyList()))
  }

  @Test
  fun `isTestFilePath - Kotlin test patterns`() {
    assertTrue(FileEntry.isTestFilePath("MainTest.kt", emptyList()))
    assertTrue(FileEntry.isTestFilePath("MainTests.kt", emptyList()))
  }

  @Test
  fun `isTestFilePath - init in test dir`() {
    assertTrue(FileEntry.isTestFilePath("test/__init__.py", emptyList()))
    assertTrue(FileEntry.isTestFilePath("src/test/__init__.py", emptyList()))
    // Regular init not in test dir
    assertFalse(FileEntry.isTestFilePath("src/__init__.py", emptyList()))
  }

  @Test
  fun `isTestFilePath - regular files`() {
    assertFalse(FileEntry.isTestFilePath("main.py", emptyList()))
    assertFalse(FileEntry.isTestFilePath("src/utils.py", emptyList()))
    assertFalse(FileEntry.isTestFilePath("Main.java", emptyList()))
    assertFalse(FileEntry.isTestFilePath("Main.kt", emptyList()))
  }

  // ==================== createFromPath tests ====================

  @Test
  fun `createFromPath - test file gets non-visible non-propagatable metadata`() {
    val entry = FileEntry.createFromPath("content", "test/tests.py", listOf("test"))
    assertFalse(entry.isVisible)
    assertFalse(entry.isEditable)
    assertFalse(entry.isPropagatable)
  }

  @Test
  fun `createFromPath - regular file gets default metadata`() {
    val entry = FileEntry.createFromPath("content", "src/main.py", listOf("test"))
    assertTrue(entry.isVisible)
    assertTrue(entry.isEditable)
    assertTrue(entry.isPropagatable)
    assertEquals(emptyMap<String, Any>(), entry.metadata)
  }

  @Test
  fun `createFromPath - tests_py without test dir`() {
    val entry = FileEntry.createFromPath("content", "tests.py", emptyList())
    assertFalse(entry.isVisible)
    assertFalse(entry.isPropagatable)
  }
}
