# Platform breakage: causes and fixes

Shared by `/ide-update-loop` (to name a cause) and `/ide-update-fix` (to fix it). Each entry: symptom →
cause → fix. The general rules of where version-specific code goes are in "Handling Platform
Compatibility" in `CLAUDE.md`; this file holds what those rules don't say.

## Where the facts are

- The IDE a build used: `.intellijPlatform/ides/<CODE>-<version>/` (`lib/`, `plugins/`,
  `product-info.json`). Check an API in its jars (`javap -cp <jar> <class>`, or list the jar) before
  relying on it; the same check on the **oldest** supported IDE tells whether common code may use it.
- `scripts/diff_platform.py <old> <new>` lists jars that left the core classpath and modules that changed
  kind. Most compile failures of a platform update are predicted there.
- Common code compiles against every environment in `gradle-*.properties`.
  `-PenvironmentName=<N>` switches the target without editing `gradle.properties`.

## Compile

- **Unresolved reference / cannot find symbol** on a platform or bundled-plugin API: removed or renamed.
  Prefer a call that exists on every supported version over a per-version shim. Example: Rust 263 removed
  `org.rust.openapiext.toPsiFile`; the plugin calls `PsiManager.findFile`, which that helper wrapped.
- **Cannot access class** / class missing from the classpath: the class moved into a content module or a
  separate bundled plugin. Compilation needs `bundledModule("<module>")`; the build scripts'
  `bundledModulesSince(version, <branch>, ...)` adds it only from the branch where the move happened.
  Runtime needs `<module name="..."/>` in the content module's descriptor. A module with
  `visibility="internal"` cannot be a dependency at all: find a public route (example: `UpdateChecker`
  went internal in 263; the plugin uses its notification group id instead).
- **Signature changed inside one platform branch** (a Kotlin data class constructor, an `inline` builder):
  Kotlin inlines the unstable signature into the plugin's bytecode. Build the object from Java, which
  cannot inline Kotlin `inline` functions; see `hs-core/.../platform/OpenProjectTaskFactory.java`.
- **Version catalog**: `compileOnly` libraries in `gradle/libs.versions.toml` (kotlinx-serialization,
  annotations) come from the IDE at runtime, so their version must not exceed what the oldest supported IDE
  bundles. The catalog is shared by all environments. Shipped (`implementation`) libraries may be newer.
- The IntelliJ Platform Gradle plugin sometimes rewrites `.intellijPlatform/localPlatformArtifacts/<IDE>/
  bundledModule-*.xml` with empty dependencies, and transitive classes vanish between runs. Declare every
  module the code uses directly rather than relying on transitivity.

## Tests

- A run with `--tests` is not comparable with a full run: platform tests leak state between classes.
  Confirm a fix with the same task set that failed.
- Bundled plugins of the test IDE all land on the test classpath. One that breaks there is excluded via
  `org.jetbrains.intellij.platform.testIdeBundledPluginsClasspathExcludes` in `gradle.properties`; the
  property replaces the default list, so keep the existing entries when adding one.
- Marketplace plugins (Python, Rust, Go, PHP, Scala) resolve as `:latest` compatible with the target IDE,
  so a bump can change them too. Rust builds are per OS since 263.

## Verify (Plugin Verifier)

- **"Package not found"** for an optional-plugin API (`com.jetbrains.php.*`, `org.rust.*`, ...), reported
  against hs-core: a lambda passed to an hs-core `inline` function (`invokeLater`,
  `runReadActionInSmartMode`) calls that API, and the inlined class is attributed to hs-core. Make the
  lambda body a single call to a private non-inline method that holds the optional-plugin calls.
- Internal, experimental and deprecated API usages are reported but do not fail the build (see
  `pluginVerification.failureLevel` in `intellij-plugin/build.gradle.kts`); a growing count is a note for
  the report, not a red item.

## Runtime

Tests run on one flat classpath; compile sees every module jar. Neither notices a class that is reachable
only through a content module's own classloader. When `diff_platform.py` shows a module the plugin uses
changing kind or leaving the core classpath (debugger and execution modules of the Java plugin did on
2026.2), the item's fix needs a runtime check:

1. `./gradlew -PenvironmentName=<N> :intellij-plugin:runIdea --args="inspect <emptyProjectDir> <profile.xml> <outDir> -v2"`
   starts the sandbox IDE headless with the real classloaders, opens the project and exits. The project
   needs only an empty `.idea/`; a minimal profile is
   `<component name="InspectionProjectProfileManager"><profile version="1.0"><option name="myName" value="Empty"/></profile></component>`.
2. Search `intellij-plugin/build/idea-sandbox/*/log_runIdea/idea.log` for `ClassNotFoundException` and
   `NoClassDefFoundError`. A content module whose dependency is unresolved is skipped silently, so "no
   error" proves less than a class-load log (`-Xlog:class+load`) showing the class loaded.

Use a release IDE for this: EAP builds expire ("This IDE build has expired").
