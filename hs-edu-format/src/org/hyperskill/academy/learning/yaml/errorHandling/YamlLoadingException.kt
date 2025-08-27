package org.hyperskill.academy.learning.yaml.errorHandling

import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.message

class YamlLoadingException(override val message: String) : IllegalStateException(message)
class RemoteYamlLoadingException(val item: StudyItem, cause: Throwable) : IllegalStateException(cause)

fun loadingError(message: String): Nothing = throw YamlLoadingException(message)

fun noDirForItemMessage(name: String, itemTypeName: String = EduFormatNames.ITEM): String =
  message("yaml.editor.invalid.format.no.dir", name, itemTypeName)

fun unknownConfigMessage(configName: String): String = message("yaml.editor.invalid.format.unknown.config", configName)

fun unexpectedItemTypeMessage(itemType: String): String = message("yaml.editor.invalid.format.unexpected.type", itemType)