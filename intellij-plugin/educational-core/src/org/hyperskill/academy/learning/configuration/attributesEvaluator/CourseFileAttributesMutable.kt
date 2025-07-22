package org.hyperskill.academy.learning.configuration.attributesEvaluator

import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.CourseFileAttributes

internal class CourseFileAttributesMutable {
  fun toImmutable(): CourseFileAttributes =
    CourseFileAttributes(excludedFromArchive, inclusionPolicy)

  var excludedFromArchive: Boolean = false
  var inclusionPolicy: ArchiveInclusionPolicy = ArchiveInclusionPolicy.AUTHOR_DECISION
}