package org.hyperskill.academy.learning.courseGeneration.macro

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.courseFormat.Course

/**
 * Provides a substitution rule wrapped into [EduMacro] for given file
 * to collapse desired project-dependent substrings in run configuration xmls during course serialization
 * and expand them during course creation.
 */
interface EduMacroProvider {

  fun provideMacro(holder: CourseInfoHolder<out Course?>, file: VirtualFile): EduMacro?

  companion object {
    val EP_NAME: ExtensionPointName<EduMacroProvider> = ExtensionPointName.create("HyperskillEducational.pathMacroProvider")
  }
}
