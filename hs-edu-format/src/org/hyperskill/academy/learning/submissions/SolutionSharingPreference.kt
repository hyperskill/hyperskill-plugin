package org.hyperskill.academy.learning.submissions

import org.hyperskill.academy.learning.submissions.SolutionSharingPreference.*


/**
 * Reflects user's preference towards sharing his correct submission
 * @property NEVER never share the correct submissions
 * @property ALWAYS always share user's correct submissions
 * @property PROHIBITED user is prohibited to share
 */
enum class SolutionSharingPreference {
  NEVER, ALWAYS, PROHIBITED
}
