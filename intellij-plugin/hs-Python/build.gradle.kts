plugins {
  id("intellij-plugin-module-conventions")
}

// `branches/261|262/src/com/jetbrains/python/**` holds copies of and shims around Python plugin internals
// (`PyTargetEnvCreationManager`, the `compat.kt` files). They call package-private and `internal` members of the
// Python plugin, which is only possible from that plugin's own package, so `verifyClasses` has to allow them.
ext[VERIFY_CLASSES_ALLOWED_PACKAGES] = "com.jetbrains.python"

private val pythonPlatformModuleDependenciesMarker = "<!-- PYTHON_PLATFORM_MODULE_DEPENDENCIES -->"
private val pythonPlatformModuleDependencies = if (environmentName.toInt() >= 262) {
  listOf(
    "intellij.libraries.lucene.common",
    // In 2026.3 `intellij.python.sdk` was split, and the SDK classes the plugin uses
    // (`PythonSdkAdditionalData`, `PythonSdkFlavor`, ...) moved to `intellij.python.sdk.backend`
    if (environmentName.toInt() >= 263) "intellij.python.sdk.backend" else "intellij.python.sdk",
  ).joinToString("\n") { "    <module name=\"$it\"/>" }
}
else {
  ""
}

dependencies {
  intellijPlatform {
    // needed to load `org.toml.lang plugin` for Python plugin in tests
    val ideVersion = if (isRiderIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)
    bundledModulesSince(ideVersion, 262, "intellij.platform.smRunner", "intellij.platform.testRunner")
    // `PyTestEventsConverter` overrides the SM runner API that exposes the TeamCity service messages library.
    // Since 2026.3 it is not part of the core classpath. Its module is internal, so it can't be declared
    // in the module descriptor and is reached at runtime through `intellij.platform.smRunner`
    bundledModulesSince(ideVersion, 263, "intellij.libraries.teamcity.service.messages")
    val ideBranch = platformBranch(ideVersion)
    if (ideBranch >= 262) {
      // In 2026.2 these libraries moved under separate bundled plugins. PythonCore
      // reaches them through JSON/spellchecker and its JCEF integration, so their
      // owning plugins must be loaded in the test sandbox.
      testBundledPlugins("com.intellij.modules.jcef")
      if (ideBranch >= 263) {
        // 2026.3 dissolved `intellij.libraries.misc.plugin`: its library modules became product modules of the core,
        // and the Structure View plugin was renamed (the old ID is only kept as an alias)
        testBundledPlugins("com.intellij.structureView")
      }
      else {
        testBundledPlugins("intellij.libraries.misc.plugin", "intellij.structureView.plugin")
      }
    }

    intellijPlugins(pythonPlugin)
    testIntellijPlugins(tomlPlugin)
    testIntellijPlatformFramework(project)
  }

  implementation(project(":intellij-plugin:hs-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}

tasks.processResources {
  inputs.property("pythonPlatformModuleDependencies", pythonPlatformModuleDependencies)
  filesMatching("hs-Python.xml") {
    filter { line ->
      if (pythonPlatformModuleDependenciesMarker in line) pythonPlatformModuleDependencies else line
    }
  }
}
