plugins {
  id("intellij-plugin-module-conventions")
}

private val pythonPlatformModuleDependenciesMarker = "<!-- PYTHON_PLATFORM_MODULE_DEPENDENCIES -->"
private val pythonPlatformModuleDependencies = if (environmentName.toInt() >= 262) {
  listOf(
    "intellij.libraries.lucene.common",
    "intellij.python.sdk",
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
    if (platformBranch(ideVersion) >= 262) {
      // In 2026.2 these libraries moved under separate bundled plugins. PythonCore
      // reaches them through JSON/spellchecker and its JCEF integration, so their
      // owning plugins must be loaded in the test sandbox.
      testBundledPlugins(
        "intellij.libraries.misc.plugin",
        "com.intellij.modules.jcef",
        "intellij.structureView.plugin",
      )
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
