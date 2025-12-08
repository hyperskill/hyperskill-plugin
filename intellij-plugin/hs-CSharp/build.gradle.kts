plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(riderVersion)
    intellijPlugins(csharpPlugins)

    bundledModule("intellij.rider")
  }

  implementation(project(":intellij-plugin:hs-core"))
  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}

tasks {
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
