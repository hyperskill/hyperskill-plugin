package org.hyperskill.academy.learning.storage

import org.hyperskill.academy.learning.courseFormat.BinaryContents
import org.hyperskill.academy.learning.courseFormat.TextualContents
import org.hyperskill.academy.learning.courseFormat.UndeterminedContents

interface ContentsFromLearningObjectsStorage {
  val storage: LearningObjectsStorage
  val path: String
}

class TextualContentsFromLearningObjectsStorage(
  override val storage: LearningObjectsStorage,
  override val path: String
) : TextualContents, ContentsFromLearningObjectsStorage {
  override val text: String
    get() = String(storage.load(path))
}

class BinaryContentsFromLearningObjectsStorage(
  override val storage: LearningObjectsStorage,
  override val path: String
) : BinaryContents, ContentsFromLearningObjectsStorage {
  override val bytes: ByteArray
    get() = storage.load(path)
}

class UndeterminedContentsFromLearningObjectsStorage(
  override val storage: LearningObjectsStorage,
  override val path: String
) : UndeterminedContents, ContentsFromLearningObjectsStorage {
  override val textualRepresentation: String
    get() = String(storage.load(path))
}