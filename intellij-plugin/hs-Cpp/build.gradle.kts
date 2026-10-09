plugins {
  id("intellij-plugin-module-conventions")
}

// The classic language engine was removed from CLion in 2026.2, so since then tests can only run with Nova (Radler)
private val testWithRadler = platformBranch(clionVersion) >= 262

tasks {
  test {
    setClionSystemProperties(withRadler = testWithRadler)
    if (testWithRadler) {
      filter {
        // Nova doesn't provide C/C++ PSI to light tests, so the elements to move can't be found
        excludeTestsMatching("org.hyperskill.academy.cpp.actions.move.CppMoveHandlerTest")
        // Generating a CMake project starts the Nova backend, which keeps using files from the temporary directory
        // of the test that started it (`JET_ADDITIONAL_DEPLOYED_PACKAGES_FILE`), so the following tests fail with its errors
        excludeTestsMatching("org.hyperskill.academy.cpp.courseGeneration.CppCourseBuilderTest")
      }
    }
  }
}

dependencies {
  intellijPlatform {
    intellijIde(clionVersion)
    // Before 2026.2 the module came to the classpath with the classic engine (`com.intellij.cidr.lang`) plugin
    bundledModulesSince(clionVersion, 262, "intellij.clion.runFile")

    intellijPlugins(cppPlugins)
    if (testWithRadler) {
      // Provides `OCLanguageStub`, the `ObjectiveC` language tests create C/C++ courses with
      testIntellijPlugins(radlerPlugin)
    }
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
