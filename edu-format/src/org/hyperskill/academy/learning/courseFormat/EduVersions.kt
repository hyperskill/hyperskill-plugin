@file:JvmName("EduVersions")

package org.hyperskill.academy.learning.courseFormat

// If you change version of any format, add point about it in `documentation/Versions.md`
const val JSON_FORMAT_VERSION: Int = 21

/**
 * Since that version we deprecated [org.hyperskill.academy.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE] and started to use
 * new [org.hyperskill.academy.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_ID] and
 * [org.hyperskill.academy.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_VERSION] properties
 *
 */
const val JSON_FORMAT_VERSION_WITH_NEW_LANGUAGE_VERSION: Int = 16