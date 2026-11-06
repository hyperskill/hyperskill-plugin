# Building the plugin for different environments

An *environment* is one IntelliJ Platform branch, selected by the `environmentName` Gradle property.
A build installs only into the branch it was built for, so supporting several IDE versions means building one zip per environment.
All IDEs of the same branch (IDEA, PyCharm, CLion, Rider, and so on) use the same zip.

## Supported environments

| `environmentName` | Platform | IDE versions and compatible builds | Compiled with |
|-------------------|----------|------------------------------------|---------------|
| `252`             | 2025.2   | `gradle-252.properties`            | JDK 21        |
| `253`             | 2025.3   | `gradle-253.properties`            | JDK 21        |
| `261`             | 2026.1   | `gradle-261.properties`            | JDK 21        |
| `262`             | 2026.2   | `gradle-262.properties`            | JDK 25        |
| `263`             | 2026.3   | `gradle-263.properties`            | JDK 25        |

## Prerequisites

- **JDK 21** to run Gradle (pinned in `.java-version`). Use the Gradle wrapper, so no separate Gradle installation is needed.
- **JDK 25** for environments `262` and later. You don't have to install it: Gradle downloads it on first use.
- **Network access** to the JetBrains repositories and Marketplace. The first build of an environment downloads IDEA, CLion and Rider of that
  branch, because language modules compile against them.
- **Disk space** for those IDEs. They are stored in `.intellijPlatform/ides`.
- **No credentials.** Only publishing needs a token, see [CI](#ci).

## Build one environment

```bash
./gradlew buildPlugin -PenvironmentName=262
```

On Windows PowerShell:

```powershell
.\gradlew buildPlugin -PenvironmentName=262
```

The result is `intellij-plugin/build/distributions/HyperskillAcademy-<pluginVersion>-<platform>.zip`,
for example `HyperskillAcademy-2026.22-2026.2.zip`.

Without `-PenvironmentName`, Gradle uses the value from `gradle.properties`.
The command-line option changes neither that file nor the project model in your IDE. To develop against another environment, edit
`environmentName` in `gradle.properties` and reload the Gradle project.

## Build all environments

Run the builds one after another, because they share the same build directories.
Each build keeps the zips of the other environments, and `clean` removes them all.

```bash
for env in 252 253 261 262 263; do
  ./gradlew buildPlugin -PenvironmentName=$env || break
done
```

On Windows PowerShell:

```powershell
foreach ($n in 252, 253, 261, 262, 263) {
  .\gradlew buildPlugin "-PenvironmentName=$n"
  if ($LASTEXITCODE -ne 0) { break }
}
```

## Build options

Pass these as `-P<name>=<value>`, or set them in `gradle.properties`.

| Property                       | Default in `gradle.properties` | Purpose                                                                                                |
|--------------------------------|--------------------------------|--------------------------------------------------------------------------------------------------------|
| `environmentName`              | the current target             | Platform branch to build for.                                                                          |
| `pluginVersion`                | the current version            | First part of the plugin version and the zip name.                                                    |
| `baseIDE`                      | `idea`                         | IDE that shared modules compile against: `idea`, `clion`, `pycharm` or `rider`.                        |
| `enableBuildSearchableOptions` | `false`                        | Builds the index that lets the plugin's settings be found by name. Makes the build slower.             |

`baseIDE` checks that shared code doesn't depend on the API of one particular IDE. It doesn't make a zip for that IDE, and it writes to the
same file name as a regular build, so don't distribute the result.

> [!WARNING]
> PowerShell splits an unquoted argument at a dot, so `-PpluginVersion=2026.22.1` reaches Gradle as `-PpluginVersion=2026` and `.22.1`.
> Quote such values: `"-PpluginVersion=2026.22.1"`.

## Check a build

Run the same checks as CI for the environment you built:

```bash
./gradlew verifyPlugin -PenvironmentName=262
```

```bash
./gradlew :intellij-plugin:test :hs-edu-format:test :hs-framework-storage:test -PenvironmentName=262
```

`verifyPlugin` checks the plugin only against the IDEA version of that environment, so it can't catch problems specific to CLion or Rider.
To try those, start the IDE with the plugin. Each environment gets its own sandbox:

```bash
./gradlew runCLion-Nova -PenvironmentName=262
```

The other run tasks are listed in [CLAUDE.md](../CLAUDE.md#build-commands).

## Install a build

1. Find your IDE's build number in **Help | About**, for example `IU-262.8665`. The first three digits are the environment.
2. Open **Settings | Plugins**, click the gear icon, and select **Install Plugin from Disk**.
3. Select the zip for that environment. Don't unpack it.

The IDE build must also be at least the `customSinceBuild` value in `gradle-<environmentName>.properties`.

## How the environment is applied

- The `net.saliman.properties` Gradle plugin loads `gradle-<environmentName>.properties`, which sets the IDE versions and the compatible build range.
- `branches/<environmentName>/src` and `branches/<environmentName>/resources` of each module are added to its sources.
- Environment `262` and later switch the Kotlin `jvmTarget` and the Java toolchain to 25, in
  `buildSrc/src/main/kotlin/intellij-plugin-common-conventions.gradle.kts`.

To add a new environment, see [SupportNewPlatformVersion.md](SupportNewPlatformVersion.md).

## CI

Each workflow has its own list of environments in its `matrix`, so a new environment has to be added to both.

| Workflow                         | Runs on                        | Environments            | Does                                       |
|----------------------------------|--------------------------------|-------------------------|--------------------------------------------|
| `.github/workflows/ci.yaml`      | Pull requests, pushes to `main` | `261`, `262`           | Runs tests and `verifyPlugin`.             |
| `.github/workflows/publish.yaml` | Successful CI on `main`, releases | `252`, `253`, `261`  | Builds, uploads the zips, and publishes.   |

`publish.yaml` versions a build as `<pluginVersion>.<run number>`. It publishes to the `dev` channel from `main` and to the stable channel from a
release. The `JB_MARKETPLACE_TOKEN` and `JB_MARKETPLACE_CHANNEL` environment variables are read only by `publishPlugin`.

## Troubleshooting

| Symptom                                                                       | Cause and fix                                                                                                                                                                                     |
|-------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| ``Property `ideaVersion` is not defined in gradle.properties``                | `environmentName` has no matching `gradle-<environmentName>.properties`. Use a value from the table above.                                                                                        |
| `class file has wrong version 69.0, should be 65.0`                           | The Kotlin `jvmTarget` and the Java toolchain got out of sync. Both blocks in `intellij-plugin-common-conventions.gradle.kts` must use the same `environmentName` threshold.                       |
| `Corrupted TAR archive`, or an `IOException` on a jar in the Gradle cache     | An earlier build was killed partway through. Run `./gradlew --stop`, then delete the file named in the error so that Gradle downloads or rebuilds it.                                              |
| The IDE reports the plugin as incompatible                                    | The zip is for another environment, or the IDE build is older than `customSinceBuild`. See [Install a build](#install-a-build).                                                                   |
| Running out of disk space                                                     | Old IDE versions stay in `.intellijPlatform/ides` after a version bump. Delete the folders that no `gradle-*.properties` file refers to.                                                           |
