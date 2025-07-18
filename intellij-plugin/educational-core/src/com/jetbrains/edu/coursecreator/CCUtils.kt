package com.jetbrains.edu.coursecreator

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.Function
import com.intellij.util.PathUtil
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle

object CCUtils {
  private val LOG = Logger.getInstance(CCUtils::class.java)

  const val GENERATED_FILES_FOLDER = ".coursecreator"
  const val DEFAULT_PLACEHOLDER_TEXT = "type here"
  private const val IS_LOCAL_COURSE: String = "Edu.IsLocalCourse"

  private val INDEX_COMPARATOR = Comparator.comparingInt(StudyItem::index)

  var Project.isLocalCourse: Boolean
    get() = PropertiesComponent.getInstance(this).getBoolean(IS_LOCAL_COURSE)
    set(value) = PropertiesComponent.getInstance(this).setValue(IS_LOCAL_COURSE, value)

  /**
   * This method decreases index and updates directory names of
   * all tasks/lessons that have higher index than specified object
   *
   * @param dirs         directories that are used to get tasks/lessons
   * @param getStudyItem function that is used to get task/lesson from VirtualFile. This function can return null
   * @param threshold    index is used as threshold
   */
  fun updateHigherElements(
    dirs: Array<VirtualFile>,
    getStudyItem: Function<VirtualFile, out StudyItem?>,
    threshold: Int,
    delta: Int
  ) {
    val itemsToUpdate = dirs
      .mapNotNull { getStudyItem.`fun`(it) }
      .filter { it.index > threshold }
      .sortedWith { item1, item2 ->
        // if we delete some dir we should start increasing numbers in dir names from the end
        -delta * INDEX_COMPARATOR.compare(item1, item2)
      }

    for (item in itemsToUpdate) {
      val newIndex = item.index + delta
      item.index = newIndex
    }
  }

  /**
   * Replaces placeholder texts with [AnswerPlaceholder.possibleAnswer]` for each task file in [course].
   * Note, it doesn't affect files in file system
   */
  fun initializeCCPlaceholders(holder: CourseInfoHolder<Course>) {
    for (item in holder.course.items) {
      when (item) {
        is Section -> initializeSectionPlaceholders(holder, item)
        is Lesson -> initializeLessonPlaceholders(holder, item)
        else -> LOG.warn("Unknown study item type: `${item.javaClass.canonicalName}`")
      }
    }
    VirtualFileManager.getInstance().refreshWithoutFileWatcher(true)
  }

  private fun initializeSectionPlaceholders(holder: CourseInfoHolder<out Course?>, section: Section) {
    for (item in section.lessons) {
      initializeLessonPlaceholders(holder, item)
    }
  }

  private fun initializeLessonPlaceholders(holder: CourseInfoHolder<out Course?>, lesson: Lesson) {
    for (task in lesson.taskList) {
      initializeTaskPlaceholders(holder, task)
    }
  }

  /**
   * Replaces placeholder texts with [AnswerPlaceholder.possibleAnswer]` for each task file in [task].
   * Note, it doesn't affect files in file system
   */
  @Suppress("UnstableApiUsage")
  fun initializeTaskPlaceholders(holder: CourseInfoHolder<out Course?>, task: Task) {
    for ((path, taskFile) in task.taskFiles) {
      if (taskFile.contents is BinaryContents) continue

      invokeAndWaitIfNeeded {
        val file = LightVirtualFile(PathUtil.getFileName(path), PlainTextFileType.INSTANCE, taskFile.contents.textualRepresentation)
        EduDocumentListener.runWithListener(holder, taskFile, file) { document ->
          initializeTaskFilePlaceholders(taskFile, document)
        }
      }
    }
  }

  private fun initializeTaskFilePlaceholders(taskFile: TaskFile, document: Document) {
    taskFile.sortAnswerPlaceholders()
    for (placeholder in taskFile.answerPlaceholders) {
      replaceAnswerPlaceholder(document, placeholder)
    }
    CommandProcessor.getInstance().executeCommand(
      null,
      { runWriteAction { FileDocumentManager.getInstance().saveDocumentAsIs(document) } },
      EduCoreBundle.message("action.create.answer.document"),
      "Edu Actions"
    )
    taskFile.text = document.text
  }

  fun replaceAnswerPlaceholder(document: Document, placeholder: AnswerPlaceholder) {
    val offset = placeholder.offset
    placeholder.placeholderText = document.getText(TextRange.create(offset, offset + placeholder.length))
    placeholder.init()

    runUndoTransparentWriteAction {
      document.replaceString(offset, offset + placeholder.length, placeholder.possibleAnswer)
      FileDocumentManager.getInstance().saveDocumentAsIs(document)
    }
  }

}
