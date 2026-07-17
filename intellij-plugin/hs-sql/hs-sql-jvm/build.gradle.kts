plugins {
  id("intellij-plugin-module-conventions")
}

private val databasePlatformModuleDependenciesMarker = "<!-- DATABASE_PLATFORM_MODULE_DEPENDENCIES -->"
private val databasePlatformModuleDependencies = if (environmentName.toInt() >= 262) {
  listOf(
    "intellij.database.impl",
  ).joinToString("\n") { "    <module name=\"$it\"/>" }
}
else {
  ""
}

dependencies {
  intellijPlatform {
    intellijIde(ideaVersion)

    intellijPlugins(jvmPlugins)
    intellijPlugins(sqlPlugins)
    // Since 2026.2 Database Tools APIs are split into content modules instead of
    // being exposed transitively by the `com.intellij.database` plugin.
    bundledModulesSince(
      ideaVersion, 262,
      "intellij.database.impl",
    )
    // Workaround to make tests work - the module is not loaded automatically
    bundledModule("com.intellij.modules.ultimate")
  }

  api(project(":intellij-plugin:hs-sql"))
  api(project(":intellij-plugin:hs-jvm-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-sql", "testOutput"))
  testImplementation(project(":intellij-plugin:hs-jvm-core", "testOutput"))
}

tasks.processResources {
  inputs.property("databasePlatformModuleDependencies", databasePlatformModuleDependencies)
  filesMatching("hs-sql-jvm.xml") {
    filter { line ->
      if (databasePlatformModuleDependenciesMarker in line) databasePlatformModuleDependencies else line
    }
  }
}
