plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)
    bundledModulesSince(ideaVersion, 262, "intellij.platform.smRunner", "intellij.platform.testRunner")

    intellijPlugins(javaScriptPlugins)
  }

  implementation(project(":intellij-plugin:hs-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}
