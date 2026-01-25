package org.hyperskill.academy.learning.framework.storage

/**
 * Represents a file entry in the framework storage snapshot.
 * Contains both content and metadata for a file.
 *
 * Metadata is stored as a map to allow future extensibility without breaking the format.
 * Known metadata keys:
 * - "visible" -> Boolean (default: true)
 * - "editable" -> Boolean (default: true)
 * - "propagatable" -> Boolean (default: true)
 * - "highlightLevel" -> String enum name (default: "ALL_PROBLEMS")
 *
 * @property content The file content as text
 * @property metadata Map of metadata key-value pairs
 */
data class FileEntry(
  val content: String,
  val metadata: Map<String, Any> = emptyMap()
) {
  // Convenience accessors for known metadata fields
  val isVisible: Boolean get() = metadata["visible"] as? Boolean ?: true
  val isEditable: Boolean get() = metadata["editable"] as? Boolean ?: true
  val isPropagatable: Boolean get() = metadata["propagatable"] as? Boolean ?: true
  val highlightLevel: String get() = metadata["highlightLevel"] as? String ?: "ALL_PROBLEMS"

  companion object {
    /**
     * Creates a FileEntry with standard metadata fields.
     */
    fun create(
      content: String,
      visible: Boolean = true,
      editable: Boolean = true,
      propagatable: Boolean = true,
      highlightLevel: String = "ALL_PROBLEMS"
    ): FileEntry {
      val metadata = mutableMapOf<String, Any>()
      // Only store non-default values to save space
      if (!visible) metadata["visible"] = false
      if (!editable) metadata["editable"] = false
      if (!propagatable) metadata["propagatable"] = false
      if (highlightLevel != "ALL_PROBLEMS") metadata["highlightLevel"] = highlightLevel
      return FileEntry(content, metadata)
    }

    /**
     * Creates a FileEntry by inferring metadata from file path.
     * Test files (in test directories or matching test patterns) are marked as
     * non-visible, non-editable, and non-propagatable.
     * All other files use default metadata (visible, editable, propagatable).
     *
     * @param content The file content
     * @param path The file path relative to task directory
     * @param testDirs List of test directory names (e.g., ["test", "tests"])
     */
    fun createFromPath(
      content: String,
      path: String,
      testDirs: List<String> = emptyList()
    ): FileEntry {
      val isTestFile = isTestFilePath(path, testDirs)
      return if (isTestFile) {
        create(content, visible = false, editable = false, propagatable = false)
      } else {
        FileEntry(content) // defaults: visible=true, editable=true, propagatable=true
      }
    }

    /**
     * Checks if a file path represents a test file based on directory and naming patterns.
     */
    fun isTestFilePath(path: String, testDirs: List<String> = emptyList()): Boolean {
      // Check if file is in a test directory
      val isInTestDir = testDirs.any { testDir ->
        path.startsWith("$testDir/") || path == testDir
      }
      if (isInTestDir) return true

      // Check for common test file patterns
      val fileName = path.substringAfterLast('/')
      return fileName == "tests.py" ||
             fileName.startsWith("test_") ||
             fileName.endsWith("_test.py") ||
             fileName.endsWith("Test.java") ||
             fileName.endsWith("Test.kt") ||
             fileName.endsWith("Tests.java") ||
             fileName.endsWith("Tests.kt") ||
             fileName == "__init__.py" && (path.contains("/test") || path.startsWith("test"))
    }
  }
}
