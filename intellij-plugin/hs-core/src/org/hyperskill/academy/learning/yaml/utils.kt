package org.hyperskill.academy.learning.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.getTaskDirectory
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.remoteConfigFileName

/**
 * Returns the directory where yaml config files are stored.
 *
 * In most cases it's the same as [getDir] except the case when it's [Task] inside [FrameworkLesson] in student mode.
 * In this case, all meta files, including config ones, are stored separately
 */
fun StudyItem.getConfigDir(project: Project): VirtualFile {
  val configDir = if (this is Task) getTaskDirectory(project) else getDir(project.courseDir)
  return configDir ?: error("Config dir for `$name` not found")
}

fun StudyItem.remoteConfigFile(project: Project): VirtualFile? {
  return getConfigDir(project).findChild(remoteConfigFileName)
}
