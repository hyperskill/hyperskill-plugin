package org.hyperskill.academy.socialMedia

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import org.hyperskill.academy.learning.EduTestAware
import org.hyperskill.academy.learning.isUnitTestMode

/**
 * Stores the user preference whether to show the "share your achievement" dialog
 * on Hyperskill project completion. Disabled permanently once the user checks "Don't ask again".
 */
@Service(Service.Level.APP)
@State(name = "HyperskillSocialMediaSettings", storages = [Storage("hyperskill.xml")])
class SocialMediaSettings : SimplePersistentStateComponent<SocialMediaSettings.SocialMediaState>(SocialMediaState()), EduTestAware {

  // Don't use property delegation like `var askToPost by state::askToPost`.
  // It doesn't work because `state` may change but delegation keeps the initial state object
  var askToPost: Boolean
    get() = state.askToPost
    set(value) {
      state.askToPost = value
    }

  override fun cleanUpState() {
    askToPost = !isUnitTestMode
  }

  companion object {
    fun getInstance(): SocialMediaSettings = service()
  }

  class SocialMediaState : BaseState() {
    var askToPost: Boolean by property(!isUnitTestMode)
  }
}
