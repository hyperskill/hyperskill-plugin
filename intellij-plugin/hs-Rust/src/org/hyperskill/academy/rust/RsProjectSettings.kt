package org.hyperskill.academy.rust

import org.hyperskill.academy.learning.newproject.EduProjectSettings
import org.rust.cargo.toolchain.RsToolchainBase

data class RsProjectSettings(val toolchain: RsToolchainBase? = null) : EduProjectSettings
