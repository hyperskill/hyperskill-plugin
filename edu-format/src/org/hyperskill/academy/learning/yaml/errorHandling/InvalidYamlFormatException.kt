package org.hyperskill.academy.learning.yaml.errorHandling

import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.courseFormat.message


class InvalidYamlFormatException(override val message: String) : IllegalStateException(message)

fun formatError(message: String): Nothing = throw InvalidYamlFormatException(message)

fun unsupportedItemTypeMessage(itemType: String, itemName: String = EduFormatNames.ITEM) = message(
  "yaml.editor.invalid.format.unsupported.type", itemName, itemType
)

fun unnamedItemAtMessage(position: Int): String = message("yaml.editor.invalid.format.unnamed.item", position)

fun negativeLengthNotAllowedMessage(): String = message("yaml.editor.invalid.format.placeholders.negative.length")

fun negativeOffsetNotAllowedMessage(): String = message("yaml.editor.invalid.format.placeholders.negative.offset")