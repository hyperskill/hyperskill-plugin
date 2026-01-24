plugins {
  id("common-conventions")
}

java {
  withSourcesJar()
}

sourceSets {
  test {
    kotlin.srcDirs("testSrc")
  }
}

dependencies {
  compileOnly(libs.kotlin.stdlib)
  compileOnly(libs.annotations)

  testImplementation(libs.kotlin.stdlib)
  testImplementation(libs.junit)
}
