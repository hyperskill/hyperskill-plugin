import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
  idea
  id("common-conventions")
}

idea {
  project {
    jdkName = "21"
    languageLevel = IdeaLanguageLevel("11")
    vcs = "Git"
  }
  module {
    excludeDirs.add(file("dependencies"))
  }
}

// Builds the plugin for several platform versions in parallel.
// `environmentName` is a single configuration-time input, so one build can only target one version.
// This task instead launches an isolated `gradlew buildPlugin` process per version in its own git
// worktree (so the shared `build/` outputs don't collide), runs them concurrently, then collects the
// resulting distributions into `build/dist-all`.
//
// Usage:
//   ./gradlew buildAllVersions                       # builds 252, 253, 261 from HEAD
//   ./gradlew buildAllVersions -Pversions=252,261    # subset
//   ./gradlew buildAllVersions -Pref=<commit|branch> # build a specific committed ref
val buildAllVersions by tasks.registering {
  group = "build"
  description = "Builds the plugin for all supported platform versions in parallel (isolated git worktrees)."

  doLast {
    val versions = (findProperty("versions") as String?)?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
      ?: listOf("252", "253", "261")
    val ref = (findProperty("ref") as String?) ?: "HEAD"
    val isWin = org.gradle.internal.os.OperatingSystem.current().isWindows
    val base = File(System.getProperty("java.io.tmpdir"), "hs-build")

    fun run(dir: File, vararg cmd: String) =
      ProcessBuilder(*cmd).directory(dir).inheritIO().start().waitFor()
        .also { if (it != 0) throw GradleException("${cmd.joinToString(" ")} failed (exit $it)") }

    // 1) Prepare an isolated worktree per version (cheap, sequential). Worktrees check out the
    //    committed `ref`, not uncommitted working-tree changes.
    val dirs = versions.associateWith { v ->
      File(base, v).also { wt ->
        if (wt.exists()) run(rootDir, "git", "worktree", "remove", "--force", wt.absolutePath)
        run(rootDir, "git", "worktree", "add", "--detach", wt.absolutePath, ref)
      }
    }

    // 2) Launch every build, THEN wait -> real parallelism. Output goes to a per-version log file to
    //    avoid interleaved console noise.
    val gradlew = if (isWin) arrayOf("cmd", "/c", "gradlew.bat") else arrayOf("./gradlew")
    logger.lifecycle("Building ${versions.joinToString(", ")} in parallel...")
    val procs = dirs.map { (v, wt) ->
      v to ProcessBuilder(*gradlew, "buildPlugin", "-PenvironmentName=$v", "--console=plain")
        .directory(wt)
        .redirectErrorStream(true)
        .redirectOutput(File(wt, "build-$v.log"))
        .start()
    }
    val failures = procs.mapNotNull { (v, p) -> v.takeIf { p.waitFor() != 0 } }

    // 3) Collect artifacts and clean up worktrees.
    val out = layout.buildDirectory.dir("dist-all").get().asFile.apply { mkdirs() }
    dirs.forEach { (v, wt) ->
      wt.walkTopDown()
        .filter { it.path.contains("build${File.separator}distributions") && it.extension == "zip" }
        .forEach { it.copyTo(File(out, "$v-${it.name}"), overwrite = true) }
      run(rootDir, "git", "worktree", "remove", "--force", wt.absolutePath)
    }

    if (failures.isNotEmpty()) {
      throw GradleException("Failed versions: ${failures.joinToString()} (see build-<version>.log in each worktree)")
    }
    logger.lifecycle("Done. Artifacts collected in $out")
  }
}


