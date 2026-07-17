plugins {
  id("intellij-plugin-module-conventions")
}

private val riderPlatformModuleDependenciesMarker = "<!-- RIDER_PLATFORM_MODULE_DEPENDENCIES -->"
private val riderPlatformModuleDependencies = if (environmentName.toInt() >= 262) {
  listOf(
    "intellij.rd.client",
    "intellij.rider.languages",
    "intellij.rider.rdclient.dotnet",
  ).joinToString("\n") { "    <module name=\"$it\"/>" }
}
else {
  ""
}

dependencies {
  intellijPlatform {
    intellijIde(riderVersion)
    intellijPlugins(csharpPlugins)

    bundledModule("intellij.rider")
    // Since 2026.2 Rider's core APIs are split into separate layout modules instead of
    // being exposed transitively by `intellij.rider`.
    bundledModulesSince(
      riderVersion, 262,
      "intellij.rd.client",
      "intellij.rider.languages",
      "intellij.rider.rdclient.dotnet",
    )
  }

  implementation(project(":intellij-plugin:hs-core"))
  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}

tasks {
  processResources {
    inputs.property("riderPlatformModuleDependencies", riderPlatformModuleDependencies)
    filesMatching("hs-CSharp.xml") {
      filter { line ->
        if (riderPlatformModuleDependenciesMarker in line) riderPlatformModuleDependencies else line
      }
    }
  }

  // Tests are disabled due to failures on TeamCity with 2025.2+
  // Rider's testFramework.jar location differs from other IDEs
  // TODO: EDU-XXXX - investigate and fix CSharp test failures
  test {
    isEnabled = false
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    if (name == "compileTestKotlin") {
      isEnabled = false
    }
  }
}
