@file:Suppress("DEPRECATION") // SolutionInitializer is deprecated, but SolutionInitializerService is not available in all supported versions

package org.hyperskill.academy.csharp.hyperskill

import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rider.projectView.SolutionDescriptionFactory
import com.jetbrains.rider.projectView.SolutionInitializer
import org.hyperskill.academy.csharp.CSharpCourseProjectGeneratorBase
import org.hyperskill.academy.learning.courseFormat.Course
import java.nio.file.Path
import kotlin.io.path.name

class CSharpHyperskillProjectGenerator(builder: CSharpHyperskillCourseBuilder, course: Course) :
  CSharpCourseProjectGeneratorBase(builder, course) {
  override fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler {
    val solutionDescription = SolutionDescriptionFactory.virtual(location.name, location.toString(), listOf())
    val strategy = RdOpenSolution(solutionDescription, false)
    SolutionInitializer.initSolution(it, strategy)
  }
}