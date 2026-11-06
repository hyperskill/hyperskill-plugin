plugins {
  id("intellij-plugin-module-conventions")
}

// Since 2026.1 the Java debugger API (`JavaLineBreakpointType`, `DebugProcessImpl`, etc.) lives in the
// `intellij.java.debugger.impl` content module of the Java plugin. In 2026.1 the module is required,
// so a dependency on the Java plugin is enough to see it, but since 2026.2 it is optional and has its own classloader,
// so it must be declared explicitly. In 2025.x these classes are part of the Java plugin's main module
private val javaDebuggerModuleDependenciesMarker = "<!-- JAVA_DEBUGGER_MODULE_DEPENDENCIES -->"
private val javaDebuggerModuleDependencies = if (environmentName.toInt() >= 261) {
  listOf(
    "intellij.java.debugger.impl",
  ).joinToString("\n") { "    <module name=\"$it\"/>" }
}
else {
  ""
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
  }

  implementation(project(":intellij-plugin:hs-core"))
  implementation(project(":intellij-plugin:hs-features:ai-debugger-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-features:ai-debugger-core", "testOutput"))
}

tasks.processResources {
  inputs.property("javaDebuggerModuleDependencies", javaDebuggerModuleDependencies)
  filesMatching("ai-debugger-jvm.xml") {
    filter { line ->
      if (javaDebuggerModuleDependenciesMarker in line) javaDebuggerModuleDependencies else line
    }
  }
}