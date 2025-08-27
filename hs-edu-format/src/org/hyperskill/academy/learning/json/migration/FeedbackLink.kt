package org.hyperskill.academy.learning.json.migration

class FeedbackLink {
  var type: LinkType = LinkType.STEPIK
  var link: String? = null

  enum class LinkType {
    STEPIK,
    CUSTOM,
    NONE
  }
}
