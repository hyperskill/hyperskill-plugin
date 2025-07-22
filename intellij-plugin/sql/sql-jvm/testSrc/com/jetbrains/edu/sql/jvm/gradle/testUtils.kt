package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.sql.psi.SqlLanguage
import org.hyperskill.academy.learning.CourseBuilder
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse

fun sqlCourse(
  courseMode: CourseMode = CourseMode.STUDENT,
  testLanguage: SqlTestLanguage = SqlTestLanguage.KOTLIN,
  buildCourse: CourseBuilder.() -> Unit
): HyperskillCourse {
  return course(
    language = SqlLanguage.INSTANCE,
    courseMode = courseMode,
    buildCourse = buildCourse
  ).apply {
    sqlTestLanguage = testLanguage
  } as HyperskillCourse
}
