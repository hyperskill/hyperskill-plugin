plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(kotlinPlugin)
  }
  implementation(project(":intellij-plugin:hs-core"))
  implementation(project(":intellij-plugin:hs-jvm-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-jvm-core", "testOutput"))
}
