plugins {
  id("intellij-plugin-module-conventions")
}

// Almost all Go plugin classes we use (`GoSdk`, `GoSdkList`, `GoSdkService`, `GoTestRunConfiguration`, etc.)
// live in its `intellij.go.impl` content module. Since 2026.3 the module is no longer embedded:
// it has its own classloader, so it must be declared explicitly, or every Go course fails with `NoClassDefFoundError`.
// In 2025.3-2026.2 the module is embedded, so a dependency on the Go plugin is enough.
// In 2025.2 the module doesn't exist, and declaring it would stop `hs-Go` from loading
private val goModuleDependenciesMarker = "<!-- GO_MODULE_DEPENDENCIES -->"
private val goModuleDependencies = if (environmentName.toInt() >= 263) {
  listOf(
    "intellij.go.impl",
  ).joinToString("\n") { "    <module name=\"$it\"/>" }
}
else {
  ""
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)
    bundledModulesSince(ideaVersion, 262, "intellij.platform.smRunner", "intellij.platform.testRunner")

    intellijPlugins(goPlugin)
    // Workaround to make tests work - the module is not loaded automatically
    bundledModule("com.intellij.modules.ultimate")
    testIntellijPlatformFramework(project)
  }

  implementation(project(":intellij-plugin:hs-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}

tasks.processResources {
  inputs.property("goModuleDependencies", goModuleDependencies)
  filesMatching("hs-Go.xml") {
    filter { line ->
      if (goModuleDependenciesMarker in line) goModuleDependencies else line
    }
  }
}
