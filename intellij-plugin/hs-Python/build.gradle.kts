plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    // needed to load `org.toml.lang plugin` for Python plugin in tests
    val ideVersion = if (isRiderIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)
    bundledModulesSince(ideVersion, 262, "intellij.platform.smRunner", "intellij.platform.testRunner")

    intellijPlugins(pythonPlugin)
    testIntellijPlugins(tomlPlugin)
    testIntellijPlatformFramework(project)
  }

  implementation(project(":intellij-plugin:hs-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}
