# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the **Hyperskill Academy** IntelliJ Platform plugin (formerly JetBrains Academy plugin). It enables educational features in
JetBrains IDEs for learning programming and creating interactive courses. The plugin integrates
with [Hyperskill](https://hi.hyperskill.org/how-we-teach).

## Build Commands

```bash
# Build the entire plugin
./gradlew buildPlugin

# Run tests for a specific module
./gradlew :intellij-plugin:hs-core:test
./gradlew :intellij-plugin:hs-Java:test

# Run a single test class
./gradlew :intellij-plugin:hs-core:test --tests "org.hyperskill.academy.learning.format.CourseFormatTest"

# Run a single test method
./gradlew :intellij-plugin:hs-core:test --tests "org.hyperskill.academy.learning.format.CourseFormatTest.testSomething"

# Run IDE with the plugin (various IDEs supported)
./gradlew runIde          # Default IDE based on baseIDE property
./gradlew runIdea         # IntelliJ IDEA Ultimate
./gradlew runPyCharm      # PyCharm
./gradlew runCLion        # CLion (classic engine)
./gradlew runCLion-Nova   # CLion (Nova engine)
./gradlew runGoLand
./gradlew runWebStorm
./gradlew runRustRover

# Verify plugin compatibility
./gradlew verifyPlugin

# Run in split mode (backend/frontend separation)
./gradlew runInSplitMode
```

## Platform Version Targeting

The plugin supports multiple IntelliJ Platform versions. The target version is controlled by `environmentName` in `gradle.properties`:

- `environmentName=252` targets 2025.2.x, `environmentName=253` targets 2025.3.x
- Version-specific properties are in `gradle-252.properties`, `gradle-253.properties`, etc.
- Branch-specific source overrides go in `branches/<version>/src/` directories

To switch versions, change `environmentName` and reload Gradle.

## Architecture

### Module Structure

- **`hs-edu-format`**: Standalone library containing course format data models (Course, Lesson, Task, etc.), YAML/JSON serialization, and
  API definitions. Has no IntelliJ dependencies.

- **`intellij-plugin/hs-core`**: Core plugin functionality - course management, task checking, UI components, Hyperskill integration

- **`intellij-plugin/hs-jvm-core`**: Shared JVM language support (Gradle, JUnit integration)

- **Language modules** (`hs-Java`, `hs-Kotlin`, `hs-Python`, `hs-Rust`, `hs-Cpp`, `hs-Go`, `hs-Php`, `hs-JavaScript`, `hs-Scala`,
  `hs-Shell`, `hs-sql`, `hs-CSharp`): Language-specific configurators and checkers

- **Feature modules** (`hs-features/*`): Optional features like AI debugger, GitHub integration, remote environments

### Source Layout

```
src/           # Main source code
resources/     # Resources
testSrc/       # Test source code
testResources/ # Test resources
branches/252/  # Platform-specific overrides for 2025.2
```

### Key Patterns

- All plugin module classes must be in the `org.hyperskill.academy` package (enforced by `verifyClasses` task)
- Kotlin stdlib is excluded from dependencies (bundled with IDE) — see `kotlin.stdlib.default.dependency=false`
- Tests use IntelliJ's test framework (`LightPlatformCodeInsightTestCase`, etc.)
- Convention plugins in `buildSrc/` provide shared Gradle configuration:
  - `common-conventions` — base Java/Kotlin setup (Java 21, Kotlin 2.1)
  - `intellij-plugin-common-conventions` — IntelliJ plugin modules with branch-specific sources
  - `intellij-plugin-module-conventions` — full IntelliJ Platform module setup with IDE caching

### IntelliJ Platform Plugin

Uses [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html) v2.x. Key
extension points:

- `intellijPlatform.pluginConfiguration` for plugin metadata
- `intellijPlatform.pluginVerification` for compatibility checks
- `intellijPlatform.caching { ides { enabled = true } }` for IDE download caching

## Handling Platform Compatibility

When code differs between platform versions:

1. **Prefer deprecated APIs** if they work across all supported versions. Add `// BACKCOMPAT: <version>` comment to mark
   for future cleanup when that version is dropped.

2. **Extract platform-specific code** to `branches/<version>/src/` directories when APIs are incompatible. Keep
   platform-specific code minimal.

3. **Use runtime checks** with `ApplicationInfo.getInstance().build` when code must compile on all platforms but behave
   differently.

4. **Platform-specific XML**: Create `platform-<name>.xml` in `branches/<version>/resources/META-INF/` and include via
   XInclude.

## Configuration Files

- `gradle.properties`: Main build configuration (plugin version, target IDE, feature flags)
- `gradle-252.properties`, `gradle-253.properties`: Platform-specific plugin/IDE versions
- `secret.properties`: OAuth client IDs (not committed, created from template)
- `gradle/libs.versions.toml`: Dependency version catalog

## Code Style

- Only English is allowed in the code (comments, identifiers, strings)

## Debugging

When running the plugin with `./gradlew runIde` (or `runIdea`, `runPyCharm`, etc.), logs are located in the **sandbox directory**, not in the system logs:

```
intellij-plugin/build/idea-sandbox/idea-sandbox-<version>/log_runIdea/idea.log
```

For example:
```
intellij-plugin/build/idea-sandbox/idea-sandbox-253/log_runIdea/idea.log
```

To view logs in real-time:
```bash
tail -f intellij-plugin/build/idea-sandbox/idea-sandbox-*/log_runIdea/idea.log
```

To search for specific log entries:
```bash
grep "YourSearchPattern" intellij-plugin/build/idea-sandbox/idea-sandbox-*/log_runIdea/idea.log
```

## Issue Tracker

Report issues to [YouTrack EDU project](https://youtrack.jetbrains.com/issues/EDU).
