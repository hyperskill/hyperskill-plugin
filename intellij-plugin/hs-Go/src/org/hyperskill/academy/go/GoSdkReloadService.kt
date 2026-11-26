package org.hyperskill.academy.go

import com.goide.sdk.GoSdk
import com.goide.sdk.combobox.GoSdkList
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import org.hyperskill.academy.go.messages.EduGoBundle
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.APP)
class GoSdkLoadService {
  private val isLoaded = AtomicBoolean(false)
  private val isStarted = AtomicBoolean(false)

  fun isLoaded(): Boolean = isLoaded.get()

  fun reloadSdk(action: (List<GoSdk>) -> Unit) {
    if (isLoaded.get() || isStarted.get()) return

    if (isStarted.compareAndSet(false, true)) {
      reloadSdks(action)
    }
  }

  @Suppress("UsagesOfObsoleteApi") // `Task.Backgroundable` is used on purpose
  private fun reloadSdks(action: (List<GoSdk>) -> Unit) =
    // The title has its own key: the Go plugin's `GoBundle` key for it was removed in 2027.1 (GO-11779)
    object : Task.Backgroundable(null, EduGoBundle.message("progress.title.discovering.go.sdks"), true) {
      override fun run(indicator: ProgressIndicator) {
        require(isStarted.get()) {
          "isStarted lock must be acquired"
        }
        GoSdkList.getInstance().reloadSdks(null) { sdkList ->
          action(sdkList)
        }
      }

      override fun onFinished() {
        isStarted.set(false)
      }

      override fun onSuccess() {
        isLoaded.set(true)
      }
    }.queue()
}