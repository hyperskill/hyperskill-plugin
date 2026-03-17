rootProject.name = "hyperskill-plugin"
include(
  "hs-edu-format",
  "hs-framework-storage",
  "intellij-plugin",
  "intellij-plugin:hs-core",
  "intellij-plugin:hs-jvm-core",
  "intellij-plugin:hs-Java",
  "intellij-plugin:hs-Kotlin",
  "intellij-plugin:hs-Scala",
  "intellij-plugin:hs-Python",
  "intellij-plugin:hs-JavaScript",
  "intellij-plugin:hs-Rust",
  "intellij-plugin:hs-Cpp",
  "intellij-plugin:hs-Cpp:CLion-Classic", // specific support for CLion classic
  "intellij-plugin:hs-Cpp:CLion-Nova",    // specific support for CLion Nova
  "intellij-plugin:hs-Go",
  "intellij-plugin:hs-Php",
  "intellij-plugin:hs-Shell",
  "intellij-plugin:hs-CSharp",
  "intellij-plugin:hs-sql",
  "intellij-plugin:hs-sql:hs-sql-jvm",
  "intellij-plugin:hs-localization",
  "intellij-plugin:hs-features:ai-debugger-core",
  "intellij-plugin:hs-features:ai-debugger-java",
  "intellij-plugin:hs-features:ai-debugger-jvm",
  "intellij-plugin:hs-features:ai-debugger-kotlin",
  "intellij-plugin:hs-features:hs-github",
)

// Note: hs-remote-env is excluded - doesn't compile with 2025.2+
// Note: downloadHyperskillCss() was removed from settings.gradle.kts
// CSS download is now done lazily in hs-core/build.gradle.kts processResources task

buildCache {
  local {
    isEnabled = true
    directory = File(rootDir, ".gradle/build-cache")
  }
}

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
  }
}
