plugins {
  id("common-conventions")
}

java {
  withSourcesJar()
}

dependencies {
  compileOnly(libs.kotlin.stdlib)
  compileOnly(libs.annotations)
  implementationWithoutKotlin(libs.jackson.module.kotlin)
  implementationWithoutKotlin(libs.jackson.dataformat.yaml)
  implementationWithoutKotlin(libs.jackson.datatype.jsr310)
  implementationWithoutKotlin(libs.retrofit)
  implementationWithoutKotlin(libs.converter.jackson)
  implementationWithoutKotlin(libs.logging.interceptor)
}

// Workaround to help java to find `module-info.java` file.
// Is there a better way?
val moduleName = "org.hyperskill.academy.format"
tasks {
  compileJava {
    inputs.property("moduleName", moduleName)
    options.compilerArgs.addAll(listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}"))
  }
}
