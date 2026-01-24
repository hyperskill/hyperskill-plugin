package org.hyperskill.academy.learning.framework.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.LessonContainer
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.framework.storage.Change
import org.hyperskill.academy.learning.framework.storage.UserChanges
import org.hyperskill.academy.learning.isToEncodeContent
import org.hyperskill.academy.learning.loadEncodedContent

typealias FLTaskState = Map<String, String>

/**
 * Get the storage ref (branch name) for a task.
 *
 * For Hyperskill project stages: "stage_<stageId>" (e.g., "stage_543")
 * For Hyperskill topic steps or other courses: "step_<stepId>" (e.g., "step_12345")
 *
 * Using stable IDs from the server ensures:
 * - No duplicate refs when multiple tasks share the same record value
 * - Consistent naming across IDE restarts
 * - Clear identification of which stage/step the storage belongs to
 */
fun Task.getStorageRef(): String {
  val course = lesson.course
  if (course is HyperskillCourse) {
    val projectLesson = course.getProjectLesson()
    if (lesson == projectLesson) {
      // Task is in project lesson - use stage ID
      val stageIndex = index - 1
      val stage = course.stages.getOrNull(stageIndex)
      if (stage != null) {
        return "stage_${stage.id}"
      }
    }
  }
  // Fallback for topics, problems, or non-Hyperskill courses - use step/task ID
  return "step_$id"
}

/**
 * Get the stage or step ID from a storage ref string.
 * Returns null if the ref doesn't match expected patterns.
 *
 * Examples:
 * - "stage_543" -> 543
 * - "step_12345" -> 12345
 */
fun parseStorageRefId(ref: String): Int? {
  return when {
    ref.startsWith("stage_") -> ref.removePrefix("stage_").toIntOrNull()
    ref.startsWith("step_") -> ref.removePrefix("step_").toIntOrNull()
    else -> null
  }
}

/**
 * Check if a storage ref is a stage ref (from Hyperskill project).
 */
fun isStageRef(ref: String): Boolean = ref.startsWith("stage_")

/**
 * Check if a storage ref is a step ref (from Hyperskill topic or other course).
 */
fun isStepRef(ref: String): Boolean = ref.startsWith("step_")

/**
 * Returns [Change]s to convert [currentState] to [targetState]
 */
fun calculateChanges(
  currentState: FLTaskState,
  targetState: FLTaskState
): UserChanges {
  val changes = mutableListOf<Change>()
  val current = HashMap(currentState)
  loop@ for ((path, nextText) in targetState) {
    val currentText = current.remove(path)
    changes += when {
      currentText == null -> Change.AddFile(path, nextText)
      currentText != nextText -> Change.ChangeFile(path, nextText)
      else -> continue@loop
    }
  }

  current.mapTo(changes) { Change.RemoveFile(it.key) }
  return UserChanges(changes)
}

fun getTaskStateFromFiles(initialFiles: Set<String>, taskDir: VirtualFile): FLTaskState {
  val documentManager = FileDocumentManager.getInstance()
  val currentState = HashMap<String, String>()
  for (path in initialFiles) {
    val file = taskDir.findFileByRelativePath(path) ?: continue

    val text = if (file.isToEncodeContent) {
      file.loadEncodedContent(isToEncodeContent = true)
    }
    else {
      runReadAction { documentManager.getDocument(file)?.text }
    }

    if (text == null) {
      continue
    }

    currentState[path] = text
  }
  return currentState
}

fun LessonContainer.visitFrameworkLessons(visit: (FrameworkLesson) -> Unit) {
  visitLessons {
    if (it is FrameworkLesson) {
      visit(it)
    }
  }
}