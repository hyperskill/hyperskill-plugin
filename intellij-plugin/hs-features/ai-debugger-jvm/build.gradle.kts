plugins {
  id("intellij-plugin-module-conventions")
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