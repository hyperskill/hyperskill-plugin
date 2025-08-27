package org.hyperskill.academy.csharp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.rider.languages.fileTypes.csharp.CSharpLanguage
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.course

class CSharpProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    if (!project.isEduProject() || project.course?.languageId != CSharpLanguage.id) return
    val manager = StudyTaskManager.getInstance(project)
    val connection = ApplicationManager.getApplication().messageBus.connect(manager)
    val listener = CSharpVirtualFileListener(project)
    connection.subscribe(VirtualFileManager.VFS_CHANGES, listener)
  }
}