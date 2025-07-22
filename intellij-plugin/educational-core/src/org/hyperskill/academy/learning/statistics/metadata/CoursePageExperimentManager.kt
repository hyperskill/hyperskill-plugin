@file:Suppress("DEPRECATION")

package org.hyperskill.academy.learning.statistics.metadata

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import kotlinx.serialization.Serializable
import org.hyperskill.academy.learning.EduTestAware
import org.hyperskill.academy.learning.statistics.metadata.CoursePageExperimentManager.ExperimentState


// BACKCOMPAT: 2025.1. Drop it
@Deprecated("Use `CourseMetadataManager` instead")
@Service(Service.Level.PROJECT)
@State(name = "CoursePageExperimentManager", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CoursePageExperimentManager : SerializablePersistentStateComponent<ExperimentState>(ExperimentState(null)), EduTestAware {

  var experiment: CoursePageExperiment?
    get() = state.experiment
    set(value) {
      updateState { ExperimentState(value) }
    }

  override fun cleanUpState() {
    experiment = null
  }

  companion object {
    fun getInstance(project: Project): CoursePageExperimentManager = project.service()
  }

  @Serializable
  data class ExperimentState(val experiment: CoursePageExperiment?)
}
