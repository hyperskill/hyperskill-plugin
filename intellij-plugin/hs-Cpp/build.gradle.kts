plugins {
  id("intellij-plugin-module-conventions")
}

tasks {
  test {
    setClionSystemProperties(withRadler = false)
  }
}

dependencies {
  intellijPlatform {
    intellijIde(clionVersion)
    // Before 2026.2 the module came to the classpath with the classic engine (`com.intellij.cidr.lang`) plugin
    bundledModulesSince(clionVersion, 262, "intellij.clion.runFile")

    intellijPlugins(cppPlugins)
    testIntellijPlatformFramework(project)
  }

  implementation(project(":intellij-plugin:hs-core"))

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))

  testImplementation(libs.junit) {
    excludeKotlinDeps()
    exclude("org.hamcrest")
  }
  testImplementationWithoutKotlin(libs.hamcrest)
}
