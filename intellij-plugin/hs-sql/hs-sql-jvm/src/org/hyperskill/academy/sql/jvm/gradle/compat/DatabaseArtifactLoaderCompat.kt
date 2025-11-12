package org.hyperskill.academy.sql.jvm.gradle.compat

import com.intellij.database.dataSource.artifacts.DatabaseArtifactList
import com.intellij.database.dataSource.artifacts.DatabaseArtifactLoader
import com.intellij.database.dataSource.artifacts.DatabaseArtifactManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project

/**
 * Compatibility helper for downloading database driver artifacts across IDE lines (252 vs 253+).
 *
 * - On older IDEs (≤252), `DatabaseArtifactLoader.downloadArtifact(ArtifactVersion)` exists.
 * - On newer IDEs (253+), the API may have changed (different method name/signature or moved to manager).
 *
 * This helper tries multiple known variants via reflection to avoid compile-time linkage
 * to 253-only APIs while keeping a single binary compatible with 252.
 */
object DatabaseArtifactLoaderCompat {
  private val LOG = Logger.getInstance(DatabaseArtifactLoaderCompat::class.java)

  /**
   * Download the given artifact using whatever API is available on the running platform.
   * Returns true if a download was attempted (or artifact considered valid), false if no suitable API was found.
   */
  @JvmStatic
  fun ensureArtifactDownloaded(project: Project, artifact: DatabaseArtifactList.ArtifactVersion): Boolean {
    // Fast-path: if already valid, nothing to download
    try {
      if (!shouldDownload(project, artifact)) return true
    }
    catch (t: Throwable) {
      // ignore and try downloading using fallbacks
    }

    // Try legacy: DatabaseArtifactLoader.downloadArtifact(ArtifactVersion)
    if (tryInvokeLoader(null, artifact)) return true

    // Try newer signatures on loader that may include Project parameter or different name
    if (tryInvokeLoader(project, artifact)) return true

    // Try manager variants: DatabaseArtifactManager.download*(Project, ArtifactVersion)
    if (tryInvokeManager(project, artifact)) return true

    LOG.warn("Failed to find a suitable API to download database artifact on this platform. Skipping.")
    return false
  }

  /**
   * Determines whether the artifact should be downloaded on the current platform.
   * If validity cannot be determined (e.g., API moved/removed), errs on the side of downloading.
   */
  @JvmStatic
  fun shouldDownload(project: Project, artifact: DatabaseArtifactList.ArtifactVersion): Boolean {
    // Try loader.isValid(artifact) if present
    try {
      val loader = DatabaseArtifactLoader.getInstance()
      val m = loader.javaClass.methods.firstOrNull { it.name == "isValid" && it.parameterTypes.size == 1 }
      if (m != null) {
        val result = m.invoke(loader, artifact) as? Boolean
        if (result != null) return !result
      }
    }
    catch (_: Throwable) {
      // fall through
    }

    // Try manager-level validity check if available
    try {
      val mgr = DatabaseArtifactManager.getInstance()
      val cls = mgr.javaClass
      val candidates = listOf(
        "isValid" to arrayOf(DatabaseArtifactList.ArtifactVersion::class.java),
        "isValid" to arrayOf(Project::class.java, DatabaseArtifactList.ArtifactVersion::class.java),
        "isDownloaded" to arrayOf(DatabaseArtifactList.ArtifactVersion::class.java),
        "isDownloaded" to arrayOf(Project::class.java, DatabaseArtifactList.ArtifactVersion::class.java),
        "hasArtifact" to arrayOf(Project::class.java, DatabaseArtifactList.ArtifactVersion::class.java)
      )
      for ((name, types) in candidates) {
        val m = cls.methods.firstOrNull { it.name == name && it.parameterTypes.contentEquals(types) } ?: continue
        val ok = when (types.size) {
          1 -> (m.invoke(mgr, artifact) as? Boolean) == true
          2 -> (m.invoke(mgr, project, artifact) as? Boolean) == true
          else -> false
        }
        return !ok
      }
    }
    catch (_: Throwable) {
      // fall through
    }

    // If we cannot determine validity, decide to download to be safe
    return true
  }

  private fun tryInvokeLoader(project: Project?, artifact: DatabaseArtifactList.ArtifactVersion): Boolean {
    return try {
      val loader = DatabaseArtifactLoader.getInstance()
      val cls = loader.javaClass

      // Known/guessed method variants to try in order
      val candidates: List<Pair<String, Array<Class<*>>>> = buildList {
        // Legacy
        add("downloadArtifact" to arrayOf(DatabaseArtifactList.ArtifactVersion::class.java))
        // Potential new names/signatures
        add("download" to arrayOf(DatabaseArtifactList.ArtifactVersion::class.java))
        add("downloadArtifact" to arrayOf(Project::class.java, DatabaseArtifactList.ArtifactVersion::class.java))
        add("download" to arrayOf(Project::class.java, DatabaseArtifactList.ArtifactVersion::class.java))
        // Batch methods – pass single item if available
        add("downloadArtifacts" to arrayOf(Project::class.java, java.util.Collection::class.java))
        add("downloadArtifacts" to arrayOf(java.util.Collection::class.java))
      }

      for ((name, paramTypes) in candidates) {
        val m = cls.methods.firstOrNull { it.name == name && matchesParams(it.parameterTypes, paramTypes) } ?: continue
        m.isAccessible = true
        when (paramTypes.size) {
          1 -> {
            val p0 = paramTypes[0]
            if (p0 == DatabaseArtifactList.ArtifactVersion::class.java) {
              m.invoke(loader, artifact)
              return true
            }
            else if (Collection::class.java.isAssignableFrom(p0)) {
              m.invoke(loader, listOf(artifact))
              return true
            }
          }

          2 -> {
            if (paramTypes[0] == Project::class.java && paramTypes[1] == DatabaseArtifactList.ArtifactVersion::class.java) {
              val p = project ?: continue
              m.invoke(loader, p, artifact)
              return true
            }
            if (paramTypes[0] == Project::class.java && Collection::class.java.isAssignableFrom(paramTypes[1])) {
              val p = project ?: continue
              m.invoke(loader, p, listOf(artifact))
              return true
            }
          }
        }
      }
      false
    }
    catch (t: Throwable) {
      // Ignore and allow other strategies
      false
    }
  }

  private fun tryInvokeManager(project: Project, artifact: DatabaseArtifactList.ArtifactVersion): Boolean {
    return try {
      val mgr = DatabaseArtifactManager.getInstance()
      val cls = mgr.javaClass

      val candidates: List<Pair<String, Array<Class<*>>>> = buildList {
        add("downloadArtifact" to arrayOf(Project::class.java, DatabaseArtifactList.ArtifactVersion::class.java))
        add("download" to arrayOf(Project::class.java, DatabaseArtifactList.ArtifactVersion::class.java))
        add("downloadArtifacts" to arrayOf(Project::class.java, java.util.Collection::class.java))
        add("ensureDownloaded" to arrayOf(Project::class.java, DatabaseArtifactList.ArtifactVersion::class.java))
        add("ensureDownloaded" to arrayOf(Project::class.java, java.util.Collection::class.java))
      }

      for ((name, paramTypes) in candidates) {
        val m = cls.methods.firstOrNull { it.name == name && matchesParams(it.parameterTypes, paramTypes) } ?: continue
        m.isAccessible = true
        when (paramTypes.size) {
          2 -> {
            if (paramTypes[1] == DatabaseArtifactList.ArtifactVersion::class.java) {
              m.invoke(mgr, project, artifact)
              return true
            }
            if (Collection::class.java.isAssignableFrom(paramTypes[1])) {
              m.invoke(mgr, project, listOf(artifact))
              return true
            }
          }
        }
      }
      false
    }
    catch (t: Throwable) {
      false
    }
  }

  private fun matchesParams(actual: Array<Class<*>>, expected: Array<Class<*>>): Boolean {
    if (actual.size != expected.size) return false
    for (i in expected.indices) {
      val e = expected[i]
      val a = actual[i]
      if (!e.isAssignableFrom(a)) return false
    }
    return true
  }
}
