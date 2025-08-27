plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(sqlPlugins)
    // Temporary workaround to make test work as expected
    // For some reason, the corresponding module is not loaded automatically
    if (isAtLeast252) {
      bundledModule("com.intellij.modules.ultimate")
    }
  }

  api(project(":intellij-plugin:hs-sql"))
  api(project(":intellij-plugin:hs-jvm-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-sql", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-jvm-core", "testOutput"))
}
