plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(clionVersion)

    intellijPlugins(cppPlugins)
  }

  implementation(project(":intellij-plugin:hs-core"))
  implementation(project(":intellij-plugin:hs-Cpp"))
  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}
