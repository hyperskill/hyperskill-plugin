package org.hyperskill.academy.ai.debugger.core.api

import com.intellij.lang.LanguageExtension
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.ai.debugger.core.utils.AIDebugUtils.runWithTests
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.ext.isTestFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task

interface TestFinder {

  fun findTestByName(project: Project, testFiles: List<VirtualFile>, testName: String): String?

  companion object {
    private val EP_NAME = LanguageExtension<TestFinder>("HyperskillEducational.testFinder")

    fun findTestByName(project: Project, task: Task, testName: String): String =
      runWithTests({
        runReadAction {
          val testFiles = task.taskFiles.values
            .filter { it.isTestFile }
            .mapNotNull { it.getVirtualFile(project) }

          if (testFiles.isEmpty()) {
            return@runReadAction null
          }

          testFiles.firstNotNullOfOrNull { testFile ->
            val testLanguage = (testFile.fileType as LanguageFileType).language
            EP_NAME.forLanguage(testLanguage)?.findTestByName(project, testFiles, testName)
          }
        }
      }) ?: error("Can't find test text for $testName")
  }
}