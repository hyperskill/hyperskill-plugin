plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(kotlinPlugin)
  }

  implementation(project(":intellij-plugin:hs-core"))
  implementation(project(":intellij-plugin:hs-features:ai-debugger-core"))
  implementation(project(":intellij-plugin:hs-features:ai-debugger-java"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-features:ai-debugger-core", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-features:ai-debugger-java", "testOutput"))
}