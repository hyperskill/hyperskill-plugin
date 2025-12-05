import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import java.net.URI

plugins {
  id("intellij-plugin-common-conventions")
  id("org.jetbrains.intellij.platform.base")
  alias(libs.plugins.kotlinSerializationPlugin)
}

// hs-core is the plugin core - it's NOT a separate module, but part of the main plugin JAR.
// Its code and resources (including hs-core.xml for XInclude) are included directly in the main plugin.
// That's why we use intellij-plugin-common-conventions instead of intellij-plugin-module-conventions.

repositories {
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
    bundledModules("intellij.platform.vcs.impl")
    testIntellijPlugins(commonTestPlugins)
    testFramework(TestFrameworkType.Bundled)
  }

  api(project(":hs-edu-format"))
  // For some reason, kotlin serialization plugin doesn't see the corresponding library from IDE dependency
  // and fails Kotlin compilation.
  // Let's provide necessary dependency during compilation to make it work
  compileOnly(libs.kotlinx.serialization) {
    excludeKotlinDeps()
  }
}

// Export test classes for other modules to use
val testOutput: Configuration by configurations.creating
dependencies {
  testOutput(sourceSets.test.get().output.classesDirs)
}

// Download Hyperskill CSS lazily (only when building, not on every Gradle invocation)
val downloadHyperskillCss by tasks.registering {
  val cssUrl = "https://hyperskill.org/static/shared.css"
  val outputFile = file("resources/style/hyperskill_task.css")
  val fallbackFile = file("../hyperskill_default.css")

  outputs.file(outputFile)
  outputs.upToDateWhen { outputFile.exists() && outputFile.length() > 0 }

  doLast {
    try {
      URI(cssUrl).toURL().openStream().use { input ->
        outputFile.parentFile.mkdirs()
        outputFile.outputStream().use { output ->
          input.copyTo(output)
        }
      }
      println("Downloaded CSS from $cssUrl")
    } catch (e: Exception) {
      println("Error downloading CSS: ${e.message}. Using local fallback.")
      fallbackFile.copyTo(outputFile, overwrite = true)
    }
  }
}

tasks.processResources {
  dependsOn(downloadHyperskillCss)
}
