plugins {
  id("intellij-plugin-module-conventions")
  alias(libs.plugins.kotlinSerializationPlugin)
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)

    bundledModules("intellij.platform.vcs.impl")
  }

  api(project(":hs-edu-format"))
  // For some reason, kotlin serialization plugin doesn't see the corresponding library from IDE dependency
  // and fails Kotlin compilation.
  // Let's provide necessary dependency during compilation to make it work
  compileOnly(libs.kotlinx.serialization) {
    excludeKotlinDeps()
  }
}

tasks {
  processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }

  processTestResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  }
}