package org.hyperskill.academy.go

import com.goide.sdk.GoSdk
import org.hyperskill.academy.learning.newproject.EduProjectSettings

data class GoProjectSettings(val sdk: GoSdk) : EduProjectSettings
