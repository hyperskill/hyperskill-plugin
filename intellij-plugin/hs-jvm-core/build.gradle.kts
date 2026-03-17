import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
    testIntellijPlatformFramework(project, TestFrameworkType.Plugin.Java)
  }

  implementation(project(":intellij-plugin:hs-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}

// hs-jvm-core contains only abstract test bases/utilities; allow zero discovered tests
tasks.test {
  // Gradle 8+: do not fail the build when no tests are discovered
  failOnNoDiscoveredTests = false
}
