package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.diagnostic.logger
import com.jetbrains.python.psi.LanguageLevel
import org.hyperskill.academy.learning.courseFormat.Course

/**
 * Name of the file pyenv and uv read the Python version of a project from. A course pins its Python version
 * by putting such a file in its root as an additional file.
 */
const val PYTHON_VERSION_FILE = ".python-version"

/**
 * Python version a course pins in its [PYTHON_VERSION_FILE].
 *
 * Only the major and minor versions are kept: the Python plugin offers one release per minor version to install,
 * so a pinned patch version could not be honoured anyway.
 */
data class PythonVersionRequirement(val major: Int, val minor: Int) {

  fun isSatisfiedBy(languageLevel: LanguageLevel): Boolean =
    languageLevel.majorVersion == major && languageLevel.minorVersion == minor

  override fun toString(): String = "$major.$minor"

  companion object {
    private val LOG = logger<PythonVersionRequirement>()

    // `3.12`, `3.12.4`, `3.13t` and the uv request forms `cpython@3.12`, `cpython-3.12.4-macos-aarch64-none`.
    // Other implementations (`pypy@3.10`), version ranges (`>=3.11`) and `system` are not supported
    private val VERSION_REGEX = Regex("""^(?:cpython[@-]?)?(\d+)\.(\d+)(?!\d)""", RegexOption.IGNORE_CASE)

    /**
     * Parses the contents of a [PYTHON_VERSION_FILE]. Like uv, only the first line that is neither blank nor a comment is read.
     *
     * Returns `null` if the file pins no version or pins a version that cannot be satisfied with a CPython interpreter.
     */
    fun parse(text: String): PythonVersionRequirement? {
      val request = text.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() && !it.startsWith("#") }
        ?: return null

      val match = VERSION_REGEX.find(request)
      if (match == null) {
        LOG.warn("Unsupported Python version request in $PYTHON_VERSION_FILE: `$request`")
        return null
      }
      val (major, minor) = match.destructured
      return PythonVersionRequirement(major.toInt(), minor.toInt())
    }
  }
}

/**
 * Python version pinned in the [PYTHON_VERSION_FILE] of the course, or `null` if the course has no such file.
 */
val Course.pythonVersionRequirement: PythonVersionRequirement?
  get() {
    val versionFile = additionalFiles.find { it.name == PYTHON_VERSION_FILE } ?: return null
    return PythonVersionRequirement.parse(versionFile.contents.textualRepresentation)
  }
