plugins {
  id("intellij-plugin-module-conventions")
  alias(libs.plugins.kotlinSerializationPlugin)
}

dependencies {
  intellijPlatform {
    intellijIde(baseVersion)
  }

  implementation(project(":intellij-plugin:hs-core"))

  // Retrofit and Jackson for AI debugger service communication
  implementationWithoutKotlin(libs.retrofit)
  implementationWithoutKotlin(libs.converter.jackson)
  implementationWithoutKotlin(libs.jackson.module.kotlin)

  api(libs.educational.ml.library.core) {
    excludeKotlinDeps()
    exclude(group = "net.java.dev.jna")
  }
  api(libs.educational.ml.library.debugger) {
    excludeKotlinDeps()
    exclude(group = "net.java.dev.jna")
  }

  compileOnly(libs.kotlinx.serialization) {
    excludeKotlinDeps()
  }

  testImplementation(project(":intellij-plugin:hs-core", "testOutput"))
}