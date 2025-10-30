package org.hyperskill.academy.ai.debugger.core.api

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.ai.debugger.core.utils.AIDebugUtils.language
import org.hyperskill.academy.ai.debugger.core.utils.AIDebugUtils.runWithTests
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.ext.isTestFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task

interface TestFinder {

  fun findTestByName(project: Project, testFiles: List<VirtualFile>, testName: String): String?

  companion object {
    private val EP_NAME = LanguageExtension<TestFinder>("HyperskillEducational.testFinder")

    fun findTestByName(project: Project, task: Task, testName: String): String =
      runWithTests(project, task, {
        runReadAction {
          EP_NAME.forLanguage(project.language())?.findTestByName(
            project,
            task.taskFiles.values.filter { it.isTestFile }.mapNotNull { it.getVirtualFile(project) },
            testName
          )
        }
      })
//        .also {
//          deleteTests(task.getInvisibleTestFiles(), project)
//        }
      ?: error("Can't find test text for $testName")
  }
}