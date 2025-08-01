package org.hyperskill.academy.learning

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import org.intellij.lang.annotations.Language
import org.junit.Assert

fun fileTree(block: FileTreeBuilder.() -> Unit): FileTree = FileTree(FileTreeBuilderImpl().apply(block).intoDirectory())

interface FileTreeBuilder {
  fun dir(path: String, block: FileTreeBuilder.() -> Unit = {})
  fun file(name: String, code: String? = null)
  fun java(name: String, @Language("JAVA") code: String) = file(name, code)
  fun kotlin(name: String, @Language("kotlin") code: String) = file(name, code)
  fun python(name: String, @Language("Python") code: String) = file(name, code)
  fun scala(name: String, @Language("Scala") code: String) = file(name, code)
}

class FileTree(private val rootDirectory: Entry.Directory) {

  fun assertEquals(baseDir: VirtualFile, fixture: CodeInsightTestFixture? = null) {
    fullyRefreshDirectory(baseDir)
    assert(rootDirectory, baseDir, fixture) { expected, actual ->
      expected == actual
    }
  }

  fun assertExists(baseDir: VirtualFile, fixture: CodeInsightTestFixture? = null) {
    fullyRefreshDirectory(baseDir)
    assert(rootDirectory, baseDir, fixture) { expected, actual ->
      actual.containsAll(expected)
    }
  }

  private fun assert(
    expected: Entry.Directory,
    actual: VirtualFile,
    fixture: CodeInsightTestFixture?,
    condition: (Set<String>, Set<String>) -> Boolean
  ) {
    val actualChildren = actual.children
      .filter { it.name !in IGNORED_FILES }
      .associateBy { it.name }
    check(condition(expected.children.keys, actualChildren.keys)) {
      "Mismatch in directory ${actual.path}\n" +
      "Expected: ${expected.children.keys}\n" +
      "Actual  : ${actualChildren.keys}"
    }

    for ((name, entry) in expected.children) {
      val child = actualChildren[name]!!
      when (entry) {
        is Entry.File -> {
          check(!child.isDirectory)
          if (entry.text != null) {
            if (fixture != null && !child.isToEncodeContent) {
              fixture.openFileInEditor(child)
              fixture.checkResult(entry.text)
            }
            else {
              val actualText = child.loadEncodedContent()
              Assert.assertEquals(entry.text, actualText)
            }
          }
        }

        is Entry.Directory -> assert(entry, child, fixture, condition)
      }
    }
  }

  fun create(root: VirtualFile) {
    fun go(dir: Entry.Directory, root: VirtualFile) {
      for ((name, entry) in dir.children) {
        when (entry) {
          is Entry.File -> {
            val vFile = root.findOrCreateChildData(root, name)
            VfsUtil.saveText(vFile, entry.text ?: "")
          }

          is Entry.Directory -> {
            go(entry, root.createChildDirectory(root, name))
          }
        }
      }
    }

    runWriteAction {
      go(rootDirectory, root)
      fullyRefreshDirectory(root)
    }
  }

  companion object {
    private val IGNORED_FILES = setOf(".idea")
  }
}

fun fullyRefreshDirectory(directory: VirtualFile) {
  VfsUtil.markDirtyAndRefresh(false, true, true, directory)
}

private class FileTreeBuilderImpl(val directory: MutableMap<String, Entry> = mutableMapOf()) : FileTreeBuilder {
  override fun dir(path: String, block: FileTreeBuilder.() -> Unit) {
    val pathSegments = path.split("/")
    forPathSegments(pathSegments, block)
  }

  override fun file(name: String, code: String?) {
    check('/' !in name) { "Bad file name `$name`" }
    val text = if (code != null) {
      // Preserve end of line at the end of the given code.
      // Otherwise, it's impossible to check any text with a newline at the end using this API
      val endsWithNewLine = code.endsWith('\n')
      val result = code.trimIndent()
      if (endsWithNewLine) result + "\n" else result
    }
    else {
      null
    }
    directory[name] = Entry.File(text)
  }

  fun intoDirectory() = Entry.Directory(directory)

  private fun forPathSegments(pathSegments: List<String>, lastBlock: FileTreeBuilder.() -> Unit) {
    directory[pathSegments[0]] = if (pathSegments.size == 1) {
      FileTreeBuilderImpl().apply(lastBlock).intoDirectory()
    }
    else {
      FileTreeBuilderImpl().apply { forPathSegments(pathSegments.drop(1), lastBlock) }.intoDirectory()
    }
  }
}

sealed class Entry {
  class File(val text: String?) : Entry()
  class Directory(val children: MutableMap<String, Entry>) : Entry()
}
