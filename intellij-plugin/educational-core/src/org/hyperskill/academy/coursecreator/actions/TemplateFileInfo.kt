package org.hyperskill.academy.coursecreator.actions

import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils

data class TemplateFileInfo(val templateName: String, val path: String, val isVisible: Boolean) {

  fun toTaskFile(params: Map<String, String> = emptyMap()): TaskFile {
    val template = GeneratorUtils.getInternalTemplateText(templateName, params)
    val taskFile = TaskFile()
    taskFile.name = path
    taskFile.contents = InMemoryTextualContents(template)
    taskFile.isVisible = isVisible
    // invisible files are considered as non-propagatable by default
    taskFile.isPropagatable = isVisible
    return taskFile
  }
}
