package org.hyperskill.academy.go.stepik.hyperskill

import org.hyperskill.academy.go.GoConfigurator
import org.hyperskill.academy.go.GoConfigurator.Companion.MAIN_GO
import org.hyperskill.academy.go.GoProjectSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator

class GoHyperskillConfigurator : HyperskillConfigurator<GoProjectSettings>(GoConfigurator()) {
  override fun getMockFileName(course: Course, text: String): String = MAIN_GO
}