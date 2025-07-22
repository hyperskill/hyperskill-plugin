package org.hyperskill.academy.jvm.courseGeneration

import com.intellij.openapi.projectRoots.impl.JavaAwareProjectJdkTableImpl
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase

abstract class JvmCourseGenerationTestBase : CourseGenerationTestBase<JdkProjectSettings>() {
  override val defaultSettings: JdkProjectSettings get() = JdkProjectSettings.emptySettings()

  override fun tearDown() {
    try {
      JavaAwareProjectJdkTableImpl.removeInternalJdkInTests()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
