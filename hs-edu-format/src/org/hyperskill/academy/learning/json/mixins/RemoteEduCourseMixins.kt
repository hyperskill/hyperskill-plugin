@file:JvmName("RemoteEduCourseMixins")
@file:Suppress("unused")

package org.hyperskill.academy.learning.json.mixins

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.DESCRIPTION_FORMAT
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.DESCRIPTION_TEXT
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.FEEDBACK_LINK
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.FILES
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.ID
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.IS_TEMPLATE_BASED
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.ITEMS
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.NAME
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.SOLUTION_HIDDEN
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TAGS
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TASK_LIST
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TASK_TYPE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TITLE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TYPE

/**
 * If you need to change something in the marketplace course archive format, you should do the following:
 * - Add description to the `hs-core/resources/marketplace/format_description.md`
 * - Create a pull request to the `https://github.com/JetBrains/intellij-plugin-verifier/tree/master/intellij-plugin-structure/structure-edu`
 * and wait for it to be accepted and deployed.
 */

@JsonPropertyOrder(TITLE, CUSTOM_NAME, TAGS, TASK_LIST, IS_TEMPLATE_BASED, TYPE)
abstract class RemoteFrameworkLessonMixin : RemoteLessonMixin() {
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(IS_TEMPLATE_BASED)
  private var isTemplateBased: Boolean = true
}

@JsonPropertyOrder(ID, TITLE, CUSTOM_NAME, TAGS, TASK_LIST, TYPE)
abstract class RemoteLessonMixin : LocalLessonMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}

@JsonPropertyOrder(ID, NAME, CUSTOM_NAME, TAGS, FILES, DESCRIPTION_TEXT, DESCRIPTION_FORMAT, FEEDBACK_LINK, SOLUTION_HIDDEN, TASK_TYPE)
abstract class RemoteTaskMixin : LocalTaskMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}

@JsonPropertyOrder(ID, TITLE, CUSTOM_NAME, TAGS, ITEMS, TYPE)
abstract class RemoteSectionMixin : LocalSectionMixin() {
  @JsonProperty(ID)
  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = IntValueFilter::class)
  private var id: Int = 0
}
