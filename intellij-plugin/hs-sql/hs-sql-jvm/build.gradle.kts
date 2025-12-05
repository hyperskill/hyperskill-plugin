plugins {
  id("intellij-plugin-module-conventions")
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(sqlPlugins)
    // Workaround to make tests work - the module is not loaded automatically
    bundledModule("com.intellij.modules.ultimate")
  }

  api(project(":intellij-plugin:hs-sql"))
  api(project(":intellij-plugin:hs-jvm-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-sql", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-jvm-core", "testOutput"))
}
