package org.hyperskill.academy.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsConfiguration
import com.jetbrains.cmake.CMakeListsFileType
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator

class CppCourseProjectGenerator(builder: CppCourseBuilder, course: Course) :
  CourseProjectGenerator<CppProjectSettings>(builder, course) {

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    if (holder.courseDir.findChild(CMakeListsFileType.FILE_NAME) != null) return emptyList()

    val mainCMakeTemplateInfo = getCppTemplates(course).mainCMakeList
    val sanitizedProjectName = FileUtil.sanitizeFileName(holder.courseDir.name)

    return listOf(
      EduFile(
        mainCMakeTemplateInfo.generatedFileName,
        mainCMakeTemplateInfo.getText(sanitizedProjectName, course.languageVersion ?: "")
      )
    ) +
           getCppTemplates(course).extraTopLevelFiles.map { templateInfo ->
             EduFile(templateInfo.generatedFileName, templateInfo.getText(sanitizedProjectName))
           }
  }

  override fun afterProjectGenerated(
    project: Project,
    projectSettings: CppProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    val googleTestSrc = FileUtil.join(project.courseDir.path, TEST_FRAMEWORKS_BASE_DIR_VALUE, GTEST_SOURCE_DIR_VALUE)
    VcsConfiguration.getInstance(project).addIgnoredUnregisteredRoots(listOf(googleTestSrc))

    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }
}
