package org.hyperskill.academy.csharp

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.CourseSetListener
import org.hyperskill.academy.learning.courseFormat.Course

class CSharpCourseSetListener(private val project: Project) : CourseSetListener {
  override fun courseSet(course: Course) {
    if (course.languageId == "C#") {
      // make sure the service is loaded (needed only for C#, not for Unity)
      CSharpBackendService.getInstance(project)
    }
  }
}