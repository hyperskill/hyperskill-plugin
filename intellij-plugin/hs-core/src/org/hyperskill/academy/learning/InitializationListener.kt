package org.hyperskill.academy.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.BrowserUtil
import com.intellij.ide.IdeBundle
import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import com.intellij.util.PlatformUtils
import org.hyperskill.academy.learning.authUtils.OAuthUtils.isBuiltinPortValid
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.yaml.YamlConfigSettings
import org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeCourse
import org.hyperskill.academy.learning.yaml.YamlMapper
import org.jetbrains.ide.BuiltInServerManager
import java.io.File

class InitializationListener : AppLifecycleListener, DynamicPluginListener {

  override fun appFrameCreated(commandLineArgs: List<String>) {
    init()
  }

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    if (pluginDescriptor.pluginId.idString == EduNames.PLUGIN_ID) {
      init()
    }
  }

  private fun init() {
    if (isUnitTestMode) return

    val port = BuiltInServerManager.getInstance().port
    if (!isBuiltinPortValid(port)) {
      notifyUnsupportedPort(port)
    }

    val propertiesComponent = PropertiesComponent.getInstance()
    if (!propertiesComponent.isValueSet(RECENT_COURSES_FILLED)) {
      fillRecentCourses()
      propertiesComponent.setValue(RECENT_COURSES_FILLED, true)
    }

    if (!propertiesComponent.isValueSet(STEPIK_AUTH_RESET)) {
      EduSettings.getInstance().user = null
      propertiesComponent.setValue(STEPIK_AUTH_RESET, true)
    }

    @Suppress("UnstableApiUsage", "DEPRECATION")
    if (!RemoteEnvHelper.isRemoteDevServer() && (PlatformUtils.isPyCharmEducational() || PlatformUtils.isIdeaEducational())) {
      showSwitchFromEduNotification()
    }
  }

  private fun showSwitchFromEduNotification() {
    EduNotificationManager.create(
      ERROR,
      EduCoreBundle.message(
        "notification.ide.switch.from.hyperskill.ide.title",
        ApplicationNamesInfo.getInstance().fullProductNameWithEdition
      ),
      EduCoreBundle.message(
        "notification.ide.switch.from.hyperskill.ide.description",
        "${ApplicationNamesInfo.getInstance().fullProductName} Community"
      ),
    ).apply {
      isSuggestionType = true
      configureDoNotAskOption(
        SWITCH_TO_COMMUNITY_DO_NOT_ASK_OPTION_ID,
        EduCoreBundle.message("notification.ide.switch.from.hyperskill.ide.do.not.ask")
      )
      addAction(
        NotificationAction.createSimple(EduCoreBundle.message("notification.ide.switch.from.hyperskill.ide.acton.title")) {
          @Suppress("UnstableApiUsage", "DEPRECATION")
          val link = if (PlatformUtils.isPyCharmEducational()) {
            "https://www.jetbrains.com/pycharm/download/"
          }
          else {
            "https://www.jetbrains.com/idea/download/"
          }
          BrowserUtil.browse(link)
          this@apply.expire()
        })
      addAction(NotificationAction.createSimple((IdeBundle.message("notifications.toolwindow.dont.show.again"))) {
        @Suppress("UnstableApiUsage")
        this@apply.setDoNotAskFor(null)
        this@apply.expire()
      })
    }.notify(null)
  }

  private fun fillRecentCourses() {
    val state = RecentProjectsManagerBase.getInstanceEx().state
    val recentPathsInfo = state.additionalInfo
    recentPathsInfo.forEach {
      val projectPath = it.key
      val course = deserializeCourse(projectPath)
      if (course != null) {
        // Note: we don't set course progress here, because we didn't load course items here
        CoursesStorage.getInstance().addCourse(course, projectPath)
      }
    }
  }

  private fun deserializeCourse(projectPath: String): Course? {
    val projectFile = File(PathUtil.toSystemDependentName(projectPath))
    val projectDir = VfsUtil.findFile(projectFile.toPath(), true) ?: return null
    val courseConfig = projectDir.findChild(YamlConfigSettings.COURSE_CONFIG) ?: return null
    return runReadAction {
      ProgressManager.getInstance().computeInNonCancelableSection<Course, Exception> {
        YamlMapper.basicMapper().deserializeCourse(VfsUtil.loadText(courseConfig))
      }
    }
  }

  private fun notifyUnsupportedPort(port: Int) {
    EduNotificationManager.create(
      ERROR,
      EduNames.JBA,
      EduCoreBundle.message("hyperskill.unsupported.port.extended.message", port.toString(), EduNames.OUTSIDE_OF_KNOWN_PORT_RANGE_URL)
    ).addAction(NotificationAction.createSimpleExpiring("Open in Browser") {
      EduBrowser.getInstance().browse(EduNames.OUTSIDE_OF_KNOWN_PORT_RANGE_URL)
    })
      .notify(null)
  }

  companion object {
    const val RECENT_COURSES_FILLED = "HyperskillEducational.recentCoursesFilled"
    const val STEPIK_AUTH_RESET = "HyperskillEducational.stepikOAuthReset"
    private const val SWITCH_TO_COMMUNITY_DO_NOT_ASK_OPTION_ID = "Edu IDEs aren't supported"
  }
}