plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    val ideVersion = if (isRiderIDE) ideaVersion else baseVersion
    intellijIde(ideVersion)

    intellijPlugins(codeWithMePlugin)
  }

  implementation(project(":intellij-plugin:hs-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}
