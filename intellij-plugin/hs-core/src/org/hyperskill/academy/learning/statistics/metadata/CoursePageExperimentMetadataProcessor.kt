package org.hyperskill.academy.learning.statistics.metadata

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseMetadataProcessor
import org.hyperskill.academy.learning.newproject.CourseProjectState

class CoursePageExperimentMetadataProcessor : CourseMetadataProcessor<CoursePageExperiment> {
  override fun findApplicableMetadata(rawMetadata: Map<String, String>): CoursePageExperiment? =
    CoursePageExperiment.fromParams(rawMetadata)

  override fun processMetadata(project: Project, course: Course, metadata: CoursePageExperiment, courseProjectState: CourseProjectState) {
    CourseSubmissionMetadataManager.getInstance(project).addMetadata(metadata.toMetadataMap())
  }
}
