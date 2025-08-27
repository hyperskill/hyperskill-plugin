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
    // TODO: investigate why it fails on TC
    isEnabled = !isAtLeast252
  }
}
