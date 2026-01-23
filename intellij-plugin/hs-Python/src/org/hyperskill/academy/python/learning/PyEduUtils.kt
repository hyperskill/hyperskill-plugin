@file:JvmName("PyEduUtils")

package org.hyperskill.academy.python.learning

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.jetbrains.python.packaging.PyPackageUtil
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.psi.LanguageLevel
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PYTHON_2_VERSION
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION
import org.hyperskill.academy.learning.courseFormat.ext.findTaskFileInDir
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.isTestsFile
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.python.learning.messages.EduPythonBundle
import org.hyperskill.academy.python.learning.newproject.PyLanguageSettings
import com.intellij.openapi.progress.Task as ProgressTask

fun Task.getCurrentTaskVirtualFile(project: Project): VirtualFile? {
  val taskDir = getDir(project.courseDir) ?: error("Failed to get task dir for `${name}` task")
  var resultFile: VirtualFile? = null
  for ((_, taskFile) in taskFiles) {
    val file = taskFile.findTaskFileInDir(taskDir) ?: continue
    if (file.isTestsFile(project) || !TextEditorProvider.isTextFile(file)) continue
    if (resultFile == null) {
      resultFile = file
    }
  }
  return resultFile
}

fun Task.getCurrentTaskFilePath(project: Project): String? {
  return getCurrentTaskVirtualFile(project)?.systemDependentPath
}

private val LOG = logger<Any>()

internal fun pythonAttributesEvaluator(baseEvaluator: AttributesEvaluator): AttributesEvaluator = AttributesEvaluator(baseEvaluator) {
  dirAndChildren(*FOLDERS_TO_EXCLUDE, direct = true) {
    excludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
  }

  extension("pyc") {
    excludeFromArchive()
    archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
  }
}

fun installRequiredPackages(project: Project, sdk: Sdk) {
  // First check if requirements.txt exists at project level
  val requirementsFile = project.courseDir.findChild("requirements.txt")
  LOG.warn("PyEduUtils: Found requirements.txt = ${requirementsFile?.path ?: "null"}")

  if (requirementsFile == null) {
    // No requirements.txt file at all
    EduNotificationManager.showWarningNotification(
      project,
      content = EduPythonBundle.message("python.dependencies.no.requirements")
    )
    return
  }

  // File exists, try to get requirements from modules for better progress reporting
  var requirements: List<PyRequirement> = emptyList()
  for (module in ModuleManager.getInstance(project).modules) {
    val moduleRequirements = runReadAction { PyPackageUtil.getRequirementsFromTxt(module) }
    if (!moduleRequirements.isNullOrEmpty()) {
      requirements = moduleRequirements
      break  // Found requirements in at least one module
    }
  }

  val packageCount = if (requirements.isNotEmpty()) "${requirements.size}" else "required"
  val packageNames = if (requirements.isNotEmpty()) {
    requirements.joinToString(", ") { it.name }
  }
  else {
    "packages from requirements.txt"
  }

  // Run installation in background with progress indicator in status bar
  ProgressManager.getInstance().run(object : ProgressTask.Backgroundable(
    project,
    EduPythonBundle.message("installing.requirements.progress"),
    true  // cancellable
  ) {
    override fun run(indicator: ProgressIndicator) {
      indicator.isIndeterminate = false
      indicator.fraction = 0.0
      indicator.text = "Preparing to install $packageCount package(s) from requirements.txt"

      try {
        val pythonPath = sdk.homePath ?: throw IllegalStateException("Python SDK path not found")
        LOG.warn("PyEduUtils: Installing from requirements.txt, SDK path = $pythonPath")
        LOG.warn("PyEduUtils: Preparing to install, python = $pythonPath, file = ${requirementsFile.path}")

        indicator.fraction = 0.1
        indicator.text = "Running: pip install -r requirements.txt ($packageNames)"

        // Execute pip install - this installs all packages at once
        val commandLine = GeneralCommandLine(pythonPath, "-m", "pip", "install", "-r", requirementsFile.path)

        // On macOS, add OpenBLAS and libomp paths for scipy compilation (both are keg-only)
        if (SystemInfo.isMac) {
          commandLine.environment["LDFLAGS"] = "-L/opt/homebrew/opt/openblas/lib -L/opt/homebrew/opt/libomp/lib -lomp"
          commandLine.environment["CPPFLAGS"] =
            "-I/opt/homebrew/opt/openblas/include -I/opt/homebrew/opt/libomp/include -Xpreprocessor -fopenmp"
          commandLine.environment["PKG_CONFIG_PATH"] = "/opt/homebrew/opt/openblas/lib/pkgconfig"
          // Tell build systems where to find OpenMP
          commandLine.environment["OpenMP_ROOT"] = "/opt/homebrew/opt/libomp"
          LOG.warn("PyEduUtils: Set OpenBLAS and libomp environment variables for macOS")
        }

        LOG.warn("PyEduUtils: Executing command: ${commandLine.commandLineString}, env: ${commandLine.environment.keys}")
        val output = ExecUtil.execAndGetOutput(commandLine)
        LOG.warn(
          "PyEduUtils: pip install output: exitCode=${output.exitCode}, stdout=${output.stdout.take(500)}, stderr=${
            output.stderr.take(
              500
            )
          }"
        )

        indicator.fraction = 0.9

        if (output.exitCode != 0) {
          throw RuntimeException("Failed to install packages: ${output.stderr}")
        }

        indicator.fraction = 1.0
        indicator.text = "Installation completed"

        // Clear file-level warning that might linger while skeletons are updating
        val editorManager = FileEditorManager.getInstance(project)
        for (module in ModuleManager.getInstance(project).modules) {
          val analyzer = DaemonCodeAnalyzerEx.getInstanceEx(module.project)
          if (editorManager.hasOpenFiles()) {
            editorManager.openFiles.forEach { file ->
              file.findPsiFile(project)?.let { psiFile ->
                analyzer.cleanFileLevelHighlights(Pass.LOCAL_INSPECTIONS, psiFile)
              }
            }
          }
        }

        // Installation completed successfully
        LOG.warn("PyEduUtils: Installation finished successfully")
        EduNotificationManager.showInfoNotification(
          project,
          content = EduPythonBundle.message("python.dependencies.installed.success")
        )
      }
      catch (e: Throwable) {
        LOG.error("Failed to install dependencies", e)
        LOG.warn("PyEduUtils: Exception type = ${e.javaClass.name}, message = ${e.message}")

        // Show detailed error message to user with OS-specific instructions
        val errorMessage = buildErrorMessage(e, sdk)
        EduNotificationManager.showErrorNotification(
          project,
          content = errorMessage
        )
      }
    }
  })
}

private fun buildErrorMessage(e: Throwable, sdk: Sdk): String {
  return when {
    // scipy/OpenBLAS related errors
    e.message?.contains("OpenBLAS") == true || e.message?.contains("scipy") == true || e.message?.contains("fopenmp") == true -> {
      // Check if using unsupported Python version
      val pythonVersion = sdk.versionString?.let { version ->
        "Python (\\d+\\.\\d+)".toRegex().find(version)?.groupValues?.get(1)
      }
      val isUnsupportedPython = pythonVersion?.let { ver ->
        val parts = ver.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        major == 3 && minor >= 14  // Python 3.14+ doesn't have pre-built scipy wheels yet
      } ?: false

      if (isUnsupportedPython) {
        """Failed to install dependencies: Python $pythonVersion is too new.
          |
          |Scientific packages (scipy, numpy) don't have pre-built wheels for Python 3.14+ yet.
          |
          |Please recreate the project with Python 3.12 or 3.13.
          |
          |Alternatively, wait for scipy to release wheels for Python 3.14.
        """.trimMargin()
      }
      else {
        when {
          SystemInfo.isMac -> {
            """Failed to install dependencies: missing system libraries.
              |
              |To compile scientific Python packages, install the required libraries:
              |  brew install openblas libomp
              |
              |Then click 'Install dependencies' again.
            """.trimMargin()
          }

          SystemInfo.isLinux -> {
            """Failed to install dependencies: missing system libraries.
              |
              |To compile scientific Python packages, install OpenBLAS:
              |
              |For Debian/Ubuntu:
              |  sudo apt-get install libopenblas-dev
              |
              |For Fedora/RHEL:
              |  sudo dnf install openblas-devel
              |
              |Then click 'Install dependencies' again.
            """.trimMargin()
          }

          SystemInfo.isWindows -> {
            """Failed to install dependencies: missing build tools.
              |
              |To compile Python packages, install Microsoft Visual C++ Build Tools:
              |  https://visualstudio.microsoft.com/visual-cpp-build-tools/
              |
              |Alternatively, ensure you're using Python 3.9+ which has better pre-built wheel support.
              |
              |Then click 'Install dependencies' again.
            """.trimMargin()
          }

          else -> {
            """Failed to install dependencies: missing system libraries.
              |
              |Please install required build dependencies for your OS (OpenBLAS, compilers).
            """.trimMargin()
          }
        }
      }
    }
    // OpenMP/libomp related errors
    e.message?.contains("fopenmp") == true || e.message?.contains("OpenMP") == true -> {
      when {
        SystemInfo.isMac -> {
          """Failed to install dependencies: OpenMP support is required.
            |
            |Please install libomp:
            |  brew install libomp
            |
            |After installation, click 'Install dependencies' again.
          """.trimMargin()
        }

        SystemInfo.isLinux -> {
          """Failed to install dependencies: OpenMP support is required.
            |
            |For Debian/Ubuntu:
            |  sudo apt-get install libomp-dev
            |
            |For Fedora/RHEL:
            |  sudo dnf install libomp-devel
            |
            |After installation, click 'Install dependencies' again.
          """.trimMargin()
        }

        else -> {
          """Failed to install dependencies: OpenMP support is required.
            |
            |Please install OpenMP library for your operating system.
          """.trimMargin()
        }
      }
    }
    // Generic pip installation errors
    e is RuntimeException && e.message?.startsWith("Failed to install packages") == true -> {
      """Failed to install dependencies.
        |
        |Error details:
        |${e.message?.take(800)}
        |
        |Tip: Check the IDE log for full error output (Help → Show Log in Finder/Explorer).
      """.trimMargin()
    }
    // Other errors
    else -> {
      """${EduPythonBundle.message("python.dependencies.installed.error")}
        |
        |Error details:
        |${e.message?.take(500)}
        |
        |Tip: Check the IDE log for full error output (Help → Show Log in Finder/Explorer).
      """.trimMargin()
    }
  }
}

fun getSupportedVersions(): List<String> {
  val pythonVersions = mutableListOf(PyLanguageSettings.ALL_VERSIONS, PYTHON_3_VERSION, PYTHON_2_VERSION)
  pythonVersions.addAll(LanguageLevel.values().map { it.toString() }.reversed())
  return pythonVersions
}


private val VirtualFile.systemDependentPath: String get() = FileUtil.toSystemDependentName(path)

private val FOLDERS_TO_EXCLUDE: Array<String> = arrayOf("__pycache__", "venv")
