plugins {
  id("intellij-plugin-module-conventions")
  alias(libs.plugins.kotlinSerializationPlugin)
}

// Since 2026.3 these modules have their own classloaders instead of being embedded into the core,
// so they require explicit dependencies. They don't exist in the layout of 2025.x at all
private val debuggerPlatformModules = if (environmentName.toInt() >= 263) {
  listOf(
    "intellij.platform.debugger",
    "intellij.platform.externalSystem",
    "intellij.platform.externalSystem.impl",
  )
}
else {
  emptyList()
}
private val debuggerPlatformModuleDependenciesMarker = "<!-- AI_DEBUGGER_PLATFORM_MODULE_DEPENDENCIES -->"
private val debuggerPlatformModuleDependencies = debuggerPlatformModules.joinToString("\n") { "    <module name=\"$it\"/>" }

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
    bundledModules(debuggerPlatformModules)
  }

  implementation(project(":intellij-plugin:hs-core"))

  // Retrofit and Jackson for AI debugger service communication
  implementationWithoutKotlin(libs.retrofit)
  implementationWithoutKotlin(libs.converter.jackson)
  implementationWithoutKotlin(libs.jackson.module.kotlin)

  api(libs.educational.ml.library.core) {
    excludeKotlinDeps()
    exclude(group = "net.java.dev.jna")
    // IntelliJ Platform bundles its own (newer) fastutil; an older transitive one
    // shadows it on the test classpath and breaks light test project initialization
    exclude(group = "it.unimi.dsi", module = "fastutil-core")
  }
  api(libs.educational.ml.library.debugger) {
    excludeKotlinDeps()
    exclude(group = "net.java.dev.jna")
    exclude(group = "it.unimi.dsi", module = "fastutil-core")
  }

  compileOnly(libs.kotlinx.serialization) {
    excludeKotlinDeps()
  }

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}

tasks.processResources {
  inputs.property("debuggerPlatformModuleDependencies", debuggerPlatformModuleDependencies)
  filesMatching("ai-debugger-core.xml") {
    filter { line ->
      if (debuggerPlatformModuleDependenciesMarker in line) debuggerPlatformModuleDependencies else line
    }
  }
}
