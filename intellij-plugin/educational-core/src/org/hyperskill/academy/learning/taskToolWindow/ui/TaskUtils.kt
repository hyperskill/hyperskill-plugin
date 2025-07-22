@file:JvmName("TaskUtils")

package org.hyperskill.academy.learning.taskToolWindow.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.IMG_TAG
import org.hyperskill.academy.learning.taskToolWindow.SCRIPT_TAG
import org.hyperskill.academy.learning.taskToolWindow.SRC_ATTRIBUTE
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.StyleManager
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

fun htmlWithResources(
  project: Project,
  content: String,
  task: Task? = project.getCurrentTask()
): String {
  val resources = StyleManager.resources(content)
  val textWithResources = GeneratorUtils.getInternalTemplateText("taskDescriptionPage.html", resources)
  return absolutizePaths(project, textWithResources, task)
}

fun wrapHintTagsInsideHTML(document: Document, wrapHint: (e: Element, number: String, title: String) -> String): Document {
  val hints = document.getElementsByClass("hint")

  val hintDefaultTitle = EduCoreBundle.message("course.creator.yaml.hint.default.title")

  fun getHintTitle(hint: Element): String {
    val actualTitleValue = hint.attr("title")
    return if (actualTitleValue == "") hintDefaultTitle else actualTitleValue
  }

  // map hint title to count of the same titles
  val countHintTitles = hints.groupingBy { getHintTitle(it) }.eachCount()

  val indexByTitle = mutableMapOf<String, Int>()

  for (hint in hints) {
    val hintTitle = getHintTitle(hint)
    val index = indexByTitle.getOrDefault(hintTitle, 0)
    val hintsWithThisTitle = countHintTitles.getValue(hintTitle)
    indexByTitle[hintTitle] = indexByTitle.getOrDefault(hintTitle, 0) + 1

    val textualIndex = if (hintsWithThisTitle <= 1) "" else (index + 1).toString()
    val hintText = wrapHint(hint, textualIndex, hintTitle)

    // we remove the title attribute, because otherwise it may generate popup hints
    hint.removeAttr("title")

    hint.html(hintText)
  }

  return document
}

private fun absolutizePaths(project: Project, content: String, task: Task?): String {
  val taskDir = task?.getDir(project.courseDir) ?: return content

  val document = Jsoup.parse(content)
  val imageElements = document.getElementsByTag(IMG_TAG)
  val scriptElements = document.getElementsByTag(SCRIPT_TAG)
  for (element in scriptElements + imageElements) {
    val src = element.attr(SRC_ATTRIBUTE)
    if (src.isNotEmpty() && !BrowserUtil.isAbsoluteURL(src)) {
      val file = File(src)
      val absolutePath = File(taskDir.path, file.path).toURI().toString()
      element.attr(SRC_ATTRIBUTE, absolutePath)
    }
  }
  return document.outerHtml()
}

fun getSortingShortcutHTML(
  upContent: String,
  downContent: String,
  attributes: String = "",
): String {
  fun String.wrap() = "<span class='shortcut-description' $attributes>$this</span>"

  val x1 = upContent.wrap()
  val y1 = downContent.wrap()
  val shift = "<label class='textShortcut'>Shift</label>".wrap()

  val selectShortcut = "$x1 / $y1"
  val reorderShortcut = "$shift $x1 / $y1"

  return EduCoreBundle.message("hyperskill.sorting.tasks.shortcut.description", selectShortcut, reorderShortcut)
}

fun canShowTerms(project: Project, task: Task): Boolean {
  return false
}