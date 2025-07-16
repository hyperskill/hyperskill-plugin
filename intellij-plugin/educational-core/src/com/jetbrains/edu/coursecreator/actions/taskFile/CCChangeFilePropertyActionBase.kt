package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project


interface State {
  fun changeState(project: Project)
  fun restoreState(project: Project)
}
