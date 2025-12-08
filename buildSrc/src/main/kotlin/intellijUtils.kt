import org.gradle.api.Project
import org.gradle.process.JavaForkOptions
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import kotlin.reflect.KProperty

const val VERIFY_CLASSES_TASK_NAME = "verifyClasses"

private const val IDE_IDEA = "idea"
private const val IDE_CLION = "clion"
private const val IDE_PYCHARM = "pycharm"
private const val IDE_RIDER = "rider"

val Project.environmentName: String by Properties

val Project.pluginVersion: String by Properties
val Project.platformVersion: String get() = "20${StringBuilder(environmentName).insert(environmentName.length - 1, '.')}"
val Project.baseIDE: String by Properties

val Project.ideaVersion: String by Properties
val Project.clionVersion: String by Properties
val Project.pycharmVersion: String by Properties
val Project.riderVersion: String by Properties

val Project.isIdeaIDE: Boolean get() = baseIDE == IDE_IDEA
val Project.isClionIDE: Boolean get() = baseIDE == IDE_CLION
val Project.isPycharmIDE: Boolean get() = baseIDE == IDE_PYCHARM
val Project.isRiderIDE: Boolean get() = baseIDE == IDE_RIDER

val Project.baseVersion: String
  get() = when {
    isIdeaIDE -> ideaVersion
    isClionIDE -> clionVersion
    isPycharmIDE -> pycharmVersion
    isRiderIDE -> riderVersion
    else -> error("Unexpected IDE name = `$baseIDE`")
  }

// Marketplace plugins with :latest by default (can be overridden in gradle.properties)
val Project.pythonProPlugin: String get() = propertyOrDefault("pythonProPlugin", "Pythonid:latest")
val Project.pythonCommunityPlugin: String get() = propertyOrDefault("pythonCommunityPlugin", "PythonCore:latest")
val Project.scalaPlugin: String get() = propertyOrDefault("scalaPlugin", "org.intellij.scala:latest")
val Project.rustPlugin: String get() = propertyOrDefault("rustPlugin", "com.jetbrains.rust:latest")
val Project.goPlugin: String get() = propertyOrDefault("goPlugin", "org.jetbrains.plugins.go:latest")
val Project.phpPlugin: String get() = propertyOrDefault("phpPlugin", "com.jetbrains.php:latest")
val Project.psiViewerPlugin: String get() = propertyOrDefault("psiViewerPlugin", "PsiViewer:latest")

val Project.pythonPlugin: String
  get() = when {
    // Since 2024.2 Python Community plugin is available in paid products (like IU) together with Python Pro as its base dependency.
    // But all necessary code that we need is inside Python Community plugin, so we need only it from compilation POV
    isIdeaIDE -> pythonCommunityPlugin
    isClionIDE -> "PythonCore"
    isPycharmIDE -> "PythonCore"
    isRiderIDE -> pythonCommunityPlugin
    else -> error("Unexpected IDE name = `$baseIDE`")
  }

// Bundled plugins (no version needed)
val Project.javaPlugin: String get() = "com.intellij.java"
val Project.kotlinPlugin: String get() = "org.jetbrains.kotlin"
val Project.tomlPlugin: String get() = "org.toml.lang"
val Project.sqlPlugin: String get() = "com.intellij.database"
val Project.shellScriptPlugin: String get() = "com.jetbrains.sh"
val Project.githubPlugin: String get() = "org.jetbrains.plugins.github"
val Project.intelliLangPlugin: String get() = "org.intellij.intelliLang"
val Project.javaScriptPlugin: String get() = "JavaScript"
val Project.nodeJsPlugin: String get() = "NodeJS"
val Project.jsonPlugin: String get() = "com.intellij.modules.json"
val Project.yamlPlugin: String get() = "org.jetbrains.plugins.yaml"
val Project.radlerPlugin: String get() = "org.jetbrains.plugins.clion.radler"
val Project.imagesPlugin: String get() = "com.intellij.platform.images"

private fun Project.propertyOrDefault(name: String, default: String): String =
  findProperty(name) as? String ?: default

val Project.jvmPlugins: List<String>
  get() = listOf(
    javaPlugin,
    "JUnit",
    "org.jetbrains.plugins.gradle"
  )

val Project.javaScriptPlugins: List<String>
  get() = listOf(
    javaScriptPlugin,
    nodeJsPlugin
  )

val Project.rustPlugins: List<String>
  get() = listOf(
    rustPlugin,
    tomlPlugin
  )

val Project.cppPlugins: List<String>
  get() = listOf(
    "com.intellij.cidr.lang",
    "com.intellij.clion",
    "com.intellij.nativeDebug",
    "org.jetbrains.plugins.clion.test.google",
    "org.jetbrains.plugins.clion.test.catch"
  )

val Project.sqlPlugins: List<String>
  get() = listOf(
    sqlPlugin,
    "intellij.grid.plugin"
  )

val Project.csharpPlugins: List<String>
  get() = listOf(
    "com.intellij.resharper.unity"
  )

// Plugins which we add to tests for all modules.
// It's the most common plugins which affect the behavior of the plugin code
val Project.commonTestPlugins: List<String>
  get() = listOf(
    imagesPlugin, // adds `svg` file type and makes IDE consider .svg files as text ones
    yamlPlugin,   // makes IDE consider .yaml files as text ones and affects formatting of yaml files
    jsonPlugin,   // dependency of a lot of other bundled plugin
  )

data class TypeWithVersion(val type: IntelliJPlatformType, val version: String)

fun String.toTypeWithVersion(): TypeWithVersion {
  val (code, version) = split("-", limit = 2)
  return TypeWithVersion(IntelliJPlatformType.fromCode(code), version)
}

fun IntelliJPlatformDependenciesExtension.intellijIde(versionWithCode: String) {
  val (type, version) = versionWithCode.toTypeWithVersion()
  create(type, version, useInstaller = false)
  // JetBrains runtime is necessary not only for running IDE but for tests as well
  jetbrainsRuntime()
}

fun IntelliJPlatformDependenciesExtension.intellijPlugins(vararg notations: String) {
  for (notation in notations) {
    when {
      notation.endsWith(":latest") -> compatiblePlugin(notation.removeSuffix(":latest"))  // pluginId:latest - auto-resolve version
      notation.contains(":") -> plugin(notation)  // pluginId:version - explicit version
      else -> bundledPlugin(notation)  // pluginId - bundled plugin
    }
  }
}

fun IntelliJPlatformDependenciesExtension.intellijPlugins(notations: List<String>) {
  intellijPlugins(*notations.toTypedArray())
}

fun IntelliJPlatformDependenciesExtension.testIntellijPlugins(vararg notations: String) {
  for (notation in notations) {
    when {
      notation.endsWith(":latest") -> compatiblePlugin(notation.removeSuffix(":latest"))  // pluginId:latest - auto-resolve version (works for tests too)
      notation.contains(":") -> testPlugin(notation)  // pluginId:version - explicit version
      else -> testBundledPlugin(notation)  // pluginId - bundled plugin
    }
  }
}

fun IntelliJPlatformDependenciesExtension.testIntellijPlugins(notations: List<String>) {
  testIntellijPlugins(*notations.toTypedArray())
}

// Since 2024.1 CLion has two sets of incompatible plugins: based on classic language engine and new one (AKA Radler).
// Platform uses `idea.suppressed.plugins.set.selector` system property to choose which plugins should be disabled.
// But there aren't `idea.suppressed.plugins.set.selector`, `idea.suppressed.plugins.set.classic`
// and `idea.suppressed.plugins.set.radler` properties in tests,
// as a result, the platform tries to load all plugins and fails because of duplicate definitions.
// Here is a workaround to make test work with CLion by defining proper values for necessary properties
fun JavaForkOptions.setClionSystemProperties(withRadler: Boolean = false) {
  val (mode, suppressedPlugins) = if (withRadler) {
    val radlerSuppressedPlugins = listOfNotNull(
      "com.intellij.cidr.lang",
      "com.intellij.cidr.lang.clangdBridge",
      "com.intellij.c.performanceTesting",
      "org.jetbrains.plugins.cidr-intelliLang",
      "com.intellij.cidr.grazie",
      "com.intellij.cidr.markdown",
    )
    "radler" to radlerSuppressedPlugins
  }
  else {
    val classicSuppressedPlugins = listOf(
      "org.jetbrains.plugins.clion.radler",
      "intellij.rider.cpp.debugger",
      "intellij.rider.plugins.clion.radler.cwm"
    )
    "classic" to classicSuppressedPlugins
  }
  systemProperty("idea.suppressed.plugins.set.selector", mode) // possible values: `classic` and `radler`
  systemProperty("idea.suppressed.plugins.set.$mode", suppressedPlugins.joinToString(","))
}

// There isn't an implicit `project` object here, so
// this is a minor workaround to use delegation for properties almost like in a regular plugin
// and not to duplicate property name twice: one time in Kotlin property and the second time in `prop` call
private object Properties {
  operator fun getValue(thisRef: Project, property: KProperty<*>): String = thisRef.prop(property.name)
}
