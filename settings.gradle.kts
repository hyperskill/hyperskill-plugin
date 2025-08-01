import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.Properties

rootProject.name = "hyperskill-plugin"
include(
  "edu-format",
  "intellij-plugin",
  "intellij-plugin:educational-core",
  "intellij-plugin:jvm-core",
  "intellij-plugin:Edu-Java",
  "intellij-plugin:Edu-Kotlin",
  "intellij-plugin:Edu-Scala",
  "intellij-plugin:Edu-Python",
  "intellij-plugin:Edu-JavaScript",
  "intellij-plugin:Edu-Rust",
  "intellij-plugin:Edu-Cpp",
  "intellij-plugin:Edu-Cpp:CLion-Classic", // specific support for CLion classic
  "intellij-plugin:Edu-Cpp:CLion-Nova",    // specific support for CLion Nova
  "intellij-plugin:Edu-Go",
  "intellij-plugin:Edu-Php",
  "intellij-plugin:Edu-Shell",
  "intellij-plugin:Edu-CSharp",
  "intellij-plugin:sql",
  "intellij-plugin:sql:sql-jvm",
  "intellij-plugin:localization",
  "intellij-plugin:features:github",
)

// BACKCOMPAT: Temporarily exclude for 2025.2 as it doesn't compile
if (settings.providers.gradleProperty("environmentName").get() != "252") {
  include("intellij-plugin:features:remote-env")
}

val secretPropertiesFilename: String = "secret.properties"

configureSecretProperties()

downloadHyperskillCss()

fun configureSecretProperties() {
  val secretProperties = file(secretPropertiesFilename)
  if (!secretProperties.exists()) {
    secretProperties.createNewFile()
  }

  val properties = loadProperties(secretPropertiesFilename)

  properties.extractAndStore(
    "intellij-plugin/educational-core/resources/stepik/stepik.properties",
    "stepikClientId",
    "cogniterraClientId",
  )
  properties.extractAndStore(
    "intellij-plugin/educational-core/resources/hyperskill/hyperskill-oauth.properties",
    "hyperskillClientId",
  )
  properties.extractAndStore(
    "intellij-plugin/educational-core/resources/twitter/oauth_twitter.properties",
    "xClientId"
  )
  properties.extractAndStore(
    "intellij-plugin/educational-core/resources/linkedin/linkedin-oauth.properties",
    "linkedInClientId",
    "linkedInClientSecret"
  )
  properties.extractAndStore(
    "edu-format/resources/aes/aes.properties",
    "aesKey"
  )
}

fun downloadHyperskillCss() {
  try {
    download(URL("https://hyperskill.org/static/shared.css"), "intellij-plugin/educational-core/resources/style/hyperskill_task.css")
  }
  catch (e: IOException) {
    System.err.println("Error downloading: ${e.message}. Using local copy")
    Files.copy(
      Paths.get("intellij-plugin/hyperskill_default.css"),
      Paths.get("intellij-plugin/educational-core/resources/style/hyperskill_task.css"),
      StandardCopyOption.REPLACE_EXISTING
    )
  }
}

fun download(url: URL, dstPath: String) {
  println("Download $url")

  url.openStream().use {
    val path = file(dstPath).toPath().toAbsolutePath()
    println("Copying file to $path")
    Files.copy(it, path, StandardCopyOption.REPLACE_EXISTING)
  }
}

fun loadProperties(path: String): Properties {
  val properties = Properties()
  file(path).bufferedReader().use { properties.load(it) }
  return properties
}

fun Properties.extractAndStore(path: String, vararg keys: String) {
  val properties = Properties()
  for (key in keys) {
    properties[key] = getProperty(key) ?: ""
  }
  val file = file(path)
  file.parentFile?.mkdirs()
  file.bufferedWriter().use { properties.store(it, "") }
}

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
