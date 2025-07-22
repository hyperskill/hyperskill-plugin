package org.hyperskill.academy.learning.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.coursecreator.yaml.createConfigFiles
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.configFileName
import org.hyperskill.academy.learning.yaml.YamlFormatSettings.YAML_TEST_PROJECT_READY
import org.hyperskill.academy.learning.yaml.YamlFormatSettings.YAML_TEST_THROW_EXCEPTION

abstract class YamlTestCase : EduTestCase() {
  override fun setUp() {
    super.setUp()

    // In this method course is set before course files are created so `CCProjectComponent.createYamlConfigFilesIfMissing` is called
    // for course with no files. This flag is checked in this method and it does nothing if the flag is false
    project.putUserData(YAML_TEST_PROJECT_READY, true)
    project.putUserData(YAML_TEST_THROW_EXCEPTION, true)

    // we need to specify the file type for yaml files as otherwise they are recognised as binaries and aren't allowed to be edited
    // we don't add dependency on yaml plugin because it's impossible to add for tests only and we don't want to have redundant dependency
    // in production code
    runWriteAction { FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE, "yaml") }
  }

  protected fun loadItemFromConfig(item: StudyItem, newConfigText: String) {
    createConfigFiles(project)
    val configFile = item.getDir(project.courseDir)!!.findChild(item.configFileName)!!
    runWriteAction {
      VfsUtil.saveText(configFile, newConfigText)
    }

    UIUtil.dispatchAllInvocationEvents()
    YamlLoader.loadItem(project, configFile, true)
  }
}
