import org.gradle.jvm.tasks.Jar
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

// Configure JAR archive name to match module descriptor name.
// IntelliJ Platform expects module JARs named as {moduleName}.jar where moduleName
// matches the module XML descriptor filename inside the JAR.
// ComposedJarTask sets archiveBaseName via convention to "{rootProject}.{project}",
// so we need to override it with a higher priority using set().
afterEvaluate {
  val descriptorName = findModuleDescriptorName()
  if (descriptorName != null) {
    tasks.named<Jar>("composedJar") {
      archiveBaseName.set(descriptorName)
    }
  }
}

fun findModuleDescriptorName(): String? {
  val resourcesDirs = sourceSets.main.get().resources.srcDirs
  for (resourcesDir in resourcesDirs) {
    if (!resourcesDir.exists()) continue
    // Module descriptors are XML files at the root of resources (not in META-INF)
    val xmlFiles = resourcesDir.listFiles { file -> file.extension == "xml" && file.isFile }
    val descriptorFile = xmlFiles?.firstOrNull() ?: continue
    return descriptorFile.nameWithoutExtension
  }
  return null
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
