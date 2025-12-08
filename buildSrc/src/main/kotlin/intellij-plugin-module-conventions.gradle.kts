import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
  id("intellij-plugin-common-conventions")
  id("org.jetbrains.intellij.platform.module")
}

repositories {
  intellijPlatform {
    defaultRepositories()
    jetbrainsRuntime()
  }
}

intellijPlatform {
  instrumentCode = false
  caching {
    ides {
      enabled = true
    }
  }
}

tasks {
  prepareSandbox { enabled = false }
  test {
    // Needed for `:intellij-plugin:hs-Kotlin`
    // Does nothing otherwise because Kotlin does not exist in the classpath
    jvmArgumentProviders += CommandLineArgumentProvider {
      listOf("-Didea.kotlin.plugin.use.k2=true")
    }
  }
}

dependencies {
  val testOutput = configurations.create("testOutput")
  testOutput(sourceSets.test.get().output.classesDirs)

  intellijPlatform {
    testIntellijPlugins(commonTestPlugins)
    testFramework(TestFrameworkType.Bundled)
  }
}
