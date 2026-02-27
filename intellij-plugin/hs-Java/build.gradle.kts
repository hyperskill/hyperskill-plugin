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
  implementation(project(":intellij-plugin:hs-jvm-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-jvm-core", "testOutput"))
}
