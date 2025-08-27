plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(phpPlugin)
    // Temporary workaround to make test work as expected
    // For some reason, the corresponding module is not loaded automatically
    if (isAtLeast252) {
      bundledModule("com.intellij.modules.ultimate")
    }
  }

  implementation(project(":intellij-plugin:hs-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}
