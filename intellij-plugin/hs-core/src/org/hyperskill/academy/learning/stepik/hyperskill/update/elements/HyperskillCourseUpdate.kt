package org.hyperskill.academy.learning.stepik.hyperskill.update.elements

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.update.elements.CourseUpdate
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.hyperskill.academy.platform.ProgressCompat
import java.util.*

class HyperskillCourseUpdate(
  override val localItem: HyperskillCourse,
  override val remoteItem: HyperskillCourse
) : CourseUpdate<HyperskillCourse>(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    baseUpdate(project)

    val remoteProject = remoteItem.hyperskillProject ?: error("'hyperskillProject' is not initialized")
    localItem.name = remoteProject.title
    localItem.description = remoteProject.description
    localItem.hyperskillProject = remoteProject

    localItem.stages = remoteItem.stages
    localItem.taskToTopics = remoteItem.taskToTopics

    localItem.updateDate = Date()
    localItem.environment = remoteItem.environment

    ProgressCompat.withBlockingIfNeeded {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(localItem)
    }
  }
}