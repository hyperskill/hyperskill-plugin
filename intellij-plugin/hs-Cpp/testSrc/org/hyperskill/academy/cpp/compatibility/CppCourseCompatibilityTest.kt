package org.hyperskill.academy.cpp.compatibility

import org.hyperskill.academy.cpp.revertNovaCodeInsightSettings
import org.hyperskill.academy.learning.compatibility.CourseCompatibilityProviderTestBase

class CppGTestCourseCompatibilityProviderTest : CourseCompatibilityProviderTestBase(CppGTestCourseCompatibilityProvider::class) {
  override fun tearDown() {
    try {
      revertNovaCodeInsightSettings()
    }
    finally {
      super.tearDown()
    }
  }
}

class CppCatchCourseCompatibilityProviderTest : CourseCompatibilityProviderTestBase(CppCatchCourseCompatibilityProvider::class) {
  override fun tearDown() {
    try {
      revertNovaCodeInsightSettings()
    }
    finally {
      super.tearDown()
    }
  }
}
