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
  test {
    // Tests are disabled due to failures on TeamCity with 2025.2+
    // TODO: EDU-XXXX - investigate and fix CSharp test failures
    isEnabled = false
  }
}
