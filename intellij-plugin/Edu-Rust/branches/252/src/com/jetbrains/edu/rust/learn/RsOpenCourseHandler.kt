package org.hyperskill.academy.rust.learn

import org.rust.cargo.toolchain.RsToolchainBase
import org.rust.ide.learn.OpenCourseHandler
import java.nio.file.Path

class RsOpenCourseHandler : OpenCourseHandler {
  override suspend fun openCourse(courseId: Int, toolchain: RsToolchainBase, projectLocation: Path?) {
    RsOpenCourseHelper.openCourse(courseId)
  }

  override fun isAlreadyStartedCourse(courseId: Int): Boolean {
    return RsOpenCourseHelper.isAlreadyStartedCourse(courseId)
  }
}
