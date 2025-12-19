package org.hyperskill.academy.learning.stepik

import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.rd.util.first
import org.hyperskill.academy.learning.configuration.EduConfiguratorManager
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL
import org.hyperskill.academy.learning.courseFormat.ext.languageById
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTaskType
import org.hyperskill.academy.learning.courseFormat.tasks.*
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask.Companion.EDU_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.IdeTask.Companion.IDE_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.OutputTask.Companion.OUTPUT_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask.Companion.THEORY_TASK_TYPE
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.xmlEscaped
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting

open class StepikTaskBuilder(private val course: Course, stepSource: StepSource) {
  private val courseType: String = course.itemType
  private val courseEnvironment: String = course.environment
  private val language: Language = course.languageById ?: Language.ANY
  private val languageVersion: String = course.languageVersion ?: ""
  private val step: Step = stepSource.block ?: error("Step is empty")
  private val stepId: Int = stepSource.id
  private val stepPosition: Int = stepSource.position
  private val updateDate = stepSource.updateDate

  private val pluginTaskTypes: Map<String, (String) -> Task> = mapOf(
    // lexicographical order
    EDU_TASK_TYPE to { name: String -> EduTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    IDE_TASK_TYPE to { name: String -> IdeTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    OUTPUT_TASK_TYPE to { name: String -> OutputTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    REMOTE_EDU_TASK_TYPE to { name: String -> RemoteEduTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    THEORY_TASK_TYPE to { name: String -> TheoryTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
  )

  private val stepikTaskBuilders: Map<String, (String) -> Task> = HyperskillTaskType.values().associateBy(
    { it.type },
    {
      when (it) {
        // lexicographical order
        HyperskillTaskType.CODE -> this::codeTask
        HyperskillTaskType.PYCHARM -> { _: String -> pycharmTask() }
        HyperskillTaskType.REMOTE_EDU -> { _: String -> pycharmTask(REMOTE_EDU_TASK_TYPE) }
        HyperskillTaskType.TEXT -> { _: String -> pycharmTask(THEORY_TASK_TYPE) }
        else -> this::unsupportedTask
      }
    })

  open fun createTask(type: String): Task {
    val taskName = HyperskillTaskType.values().find { it.type == type }?.value ?: UNKNOWN_TASK_NAME
    return (stepikTaskBuilders[type] ?: this::unsupportedTask).invoke(taskName)
  }

  private fun Step.pycharmOptions(): PyCharmStepOptions {
    return options as PyCharmStepOptions
  }

  private fun Task.fillDescription() {
    if (this !is CodeTask) return

    val options = step.pycharmOptions()
    val samples = options.samples

    descriptionFormat = DescriptionFormat.HTML
    descriptionText = buildString {
      append(step.text)

      if (samples != null) {
        append("<br>")
        for (sample in samples) {
          if (sample.size == 2) {
            append("<b>Sample Input:</b><br><pre><code class=\"language-no-highlight\">${sample[0].prepareSample()}</code></pre><br>")
            append("<b>Sample Output:</b><br><pre><code class=\"language-no-highlight\">${sample[1].prepareSample()}</code></pre><br><br>")
          }
        }
      }

      var memoryLimit = options.executionMemoryLimit
      var timeLimit = options.executionTimeLimit
      val languageSpecificLimits = options.limits
      val stepikLanguageName = StepikLanguage.langOfId(language.id, languageVersion).langName
      if (languageSpecificLimits != null && stepikLanguageName != null) {
        languageSpecificLimits[stepikLanguageName]?.let {
          memoryLimit = it.memory
          timeLimit = it.time
        }
      }
      if (memoryLimit != null && timeLimit != null) {
        append("""<br><font color="gray">${EduCoreBundle.message("stepik.memory.limit", memoryLimit)}</font>""")
        append("""<br><font color="gray">${EduCoreBundle.message("stepik.time.limit", timeLimit)}</font><br><br>""")
      }
    }
  }

  private fun codeTask(name: String): CodeTask {
    val codeTemplates = step.pycharmOptions().codeTemplates
    val (submissionLanguage, codeTemplate) = getLangAndCodeTemplate(codeTemplates.orEmpty())
    val task = CodeTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked, submissionLanguage)

    task.fillDescription()
    initTaskFiles(task, "write your answer here \n", codeTemplate)
    return task
  }

  private fun getLangAndCodeTemplate(codeTemplates: Map<String, String>): Pair<String?, String?> =
    when (codeTemplates.size) {
      0 -> null to null
      1 -> codeTemplates.entries.first().toPair()
      else -> {
        // Select the latest programming language version. We assume that the latest version has backwards compatibility.
        // For example, Java 17 and 11 or Python 3 and 3.10. See https://stepik.org/lesson/63139/step/11 for all available versions.
        // Hyperskill uses more than one version when switches to a new one
        val langWithMaxVersion = codeTemplates.keys
          .mapNotNull { langAndVersionRegex.matchEntire(it)?.groupValues }
          .reduceOrNull { max, curr -> if (VersionComparatorUtil.compare(max[2], curr[2]) > 0) max else curr }
          ?.first()

        if (langWithMaxVersion == null) codeTemplates.first().toPair()
        else langWithMaxVersion to codeTemplates[langWithMaxVersion]
      }
    }

  // We can get an unsupported task for hyperskill courses only. There only task type is important, no other info is used
  private fun unsupportedTask(@NonNls name: String): Task {
    return UnsupportedTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked).apply {
      descriptionFormat = DescriptionFormat.HTML
      initTaskFiles(this)
    }
  }

  private fun pycharmTask(type: String? = null): Task {
    val stepOptions = step.pycharmOptions()
    val taskName: String = stepOptions.title ?: DEFAULT_EDU_TASK_NAME

    val taskType = type ?: stepOptions.taskType
    val task = pluginTaskTypes[taskType]?.invoke(taskName) ?: EduTask(taskName, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.customPresentableName = stepOptions.customPresentableName
    task.solutionHidden = stepOptions.solutionHidden

    task.descriptionText = if (!stepOptions.descriptionText.isNullOrEmpty() && courseType != HYPERSKILL) {
      stepOptions.descriptionText.orEmpty()
    }
    else {
      step.text
    }
    task.descriptionFormat = stepOptions.descriptionFormat

    initTaskFiles(task)
    return task
  }

  private fun initTaskFiles(
    task: Task,
    comment: String = "You can experiment here, it won’t be checked\n",
    codeTemplate: String? = null,
  ) {
    val options = step.options
    if (options is PyCharmStepOptions) {
      options.files?.forEach {
        task.addTaskFile(it)
      }
    }

    if (task.taskFiles.isEmpty()) {
      createMockTaskFile(task, comment, codeTemplate)
    }
  }

  private fun createMockTaskFile(task: Task, comment: String, codeTemplate: String?) {
    val configurator = EduConfiguratorManager.findConfigurator(courseType, courseEnvironment, language)
    if (configurator == null) {
      LOG.error("Could not find configurator for courseType $courseType, language $language")
      return
    }
    val editorText = buildString {
      if (codeTemplate == null) {
        val commentPrefix = LanguageCommenters.INSTANCE.forLanguage(language)?.lineCommentPrefix
        if (commentPrefix != null) {
          append("$commentPrefix $comment")
        }
        append("\n${configurator.mockTemplate}")
      }
      else {
        append(codeTemplate)
      }
    }

    val fileName = configurator.getMockFileName(course, editorText)
    if (fileName == null) {
      LOG.error(
        "Failed to retrieve fileName: courseType=$courseType, languageId=${language.id}, configurator=${configurator.javaClass.simpleName}"
      )
      return
    }
    val taskFilePath = GeneratorUtils.joinPaths(configurator.sourceDir, fileName)
    val taskFile = TaskFile()
    taskFile.contents = InMemoryTextualContents(editorText)
    taskFile.name = taskFilePath
    task.addTaskFile(taskFile)
  }

  protected open fun getLanguageName(language: Language): String? {
    return StepikLanguage.langOfId(language.id, languageVersion).langName
  }

  companion object {
    private const val DEFAULT_EDU_TASK_NAME = "Edu Task"
    private const val UNKNOWN_TASK_NAME = "Unknown"
    private val LOG = Logger.getInstance(StepikTaskBuilder::class.java)
    val langAndVersionRegex = Regex("^([a-zA-Z+#]+)\\s?([.|0-9]+)$")

    @VisibleForTesting
    fun String.prepareSample(): String = xmlEscaped.replace("\n", "<br>")
  }
}
