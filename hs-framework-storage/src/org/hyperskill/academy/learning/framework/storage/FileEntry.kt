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
  }
}
