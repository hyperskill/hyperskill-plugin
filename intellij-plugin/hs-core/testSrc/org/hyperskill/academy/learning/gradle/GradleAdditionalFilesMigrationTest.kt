package org.hyperskill.academy.learning.gradle

import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseFormat.InMemoryBinaryContents
import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseFormat.InMemoryUndeterminedContents
import org.hyperskill.academy.learning.courseFormat.TextualContents
import org.hyperskill.academy.learning.courseFormat.UndeterminedContents
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleAdditionalFilesMigrationTest {

  private val legacyBuildScript = """
    configure(subprojects.findAll { it.name != 'util' }) {
        dependencies {
            testImplementation project(':util').sourceSets.main.output
        }
    }
  """.trimIndent()

  private val legacySettingsScript = """
    buildscript {
      dependencies {
        classpath "com.github.hyperskill:hs-gradle-plugin:release-SNAPSHOT"
      }
    }

    include 'util'
  """.trimIndent()

  private fun migrate(vararg files: EduFile): List<EduFile> = files.toList().also {
    GradleScriptMigration.migrateAdditionalFiles(it)
  }

  private fun eduFile(name: String, text: String) = EduFile(name, InMemoryUndeterminedContents(text))

  @Test
  fun `test gradle scripts of a course are migrated`() {
    val (buildScript, settingsScript) = migrate(
      eduFile(GradleConstants.BUILD_GRADLE, legacyBuildScript),
      eduFile(GradleConstants.SETTINGS_GRADLE, legacySettingsScript)
    )

    assertEquals(
      GradleScriptMigration.migrateLegacyUtilSourceSetReferences(legacyBuildScript),
      buildScript.contents.textualRepresentation
    )
    assertEquals(
      GradleScriptMigration.addToolchainResolver(legacySettingsScript),
      settingsScript.contents.textualRepresentation
    )
  }

  /**
   * The course updater rewrites an additional file whenever the remote content differs from the one on disk,
   * so the content the migration produces has to be stable, otherwise the file is rewritten on every update check.
   */
  @Test
  fun `test migration is idempotent`() {
    val (buildScript, settingsScript) = migrate(
      eduFile(GradleConstants.BUILD_GRADLE, legacyBuildScript),
      eduFile(GradleConstants.SETTINGS_GRADLE, legacySettingsScript)
    )
    val migratedBuildScript = buildScript.contents.textualRepresentation
    val migratedSettingsScript = settingsScript.contents.textualRepresentation

    migrate(buildScript, settingsScript)

    assertEquals(migratedBuildScript, buildScript.contents.textualRepresentation)
    assertEquals(migratedSettingsScript, settingsScript.contents.textualRepresentation)
  }

  @Test
  fun `test other additional files are left as is`() {
    val text = "testImplementation project(':util').sourceSets.main.output"
    val (file) = migrate(eduFile("build.gradle.kts", text))

    assertEquals(text, file.contents.textualRepresentation)
  }

  @Test
  fun `test binary additional files are left as is`() {
    val bytes = byteArrayOf(0, 1, 2)
    val (file) = migrate(EduFile(GradleConstants.BUILD_GRADLE, InMemoryBinaryContents(bytes)))

    assertArrayEquals(bytes, (file.contents as InMemoryBinaryContents).bytes)
  }

  @Test
  fun `test contents kind is preserved`() {
    val (undetermined, textual) = migrate(
      EduFile(GradleConstants.BUILD_GRADLE, InMemoryUndeterminedContents(legacyBuildScript)),
      EduFile(GradleConstants.BUILD_GRADLE, InMemoryTextualContents(legacyBuildScript))
    )

    assertTrue(undetermined.contents is UndeterminedContents)
    assertTrue(textual.contents is TextualContents)
  }
}
