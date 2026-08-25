package org.hyperskill.academy.learning.gradle

import org.junit.Assert.assertEquals
import org.junit.Test

class GradleLegacyBuildScriptMigrationTest {

  private fun migrate(content: String): String = GradleScriptMigration.migrateLegacyUtilSourceSetReferences(content)

  @Test
  fun `test legacy references are qualified with rootProject`() {
    val original = """
      configure(subprojects.findAll { it.name != 'util' }) {
          dependencies {
              implementation project(':util').sourceSets.main.output
              testImplementation project(':util').sourceSets.test.output
          }
      }
    """.trimIndent()
    val expected = """
      configure(subprojects.findAll { it.name != 'util' }) {
          dependencies {
              implementation rootProject.project(':util').sourceSets.main.output
              testImplementation rootProject.project(':util').sourceSets.test.output
          }
      }
    """.trimIndent()

    assertEquals(expected, migrate(original))
  }

  @Test
  fun `test double quoted references are migrated`() {
    assertEquals(
      """testImplementation rootProject.project(":util").sourceSets.main.output""",
      migrate("""testImplementation project(":util").sourceSets.main.output""")
    )
  }

  @Test
  fun `test mixed quotes are not matched`() {
    val original = """testImplementation project(':util").sourceSets.main.output"""
    assertEquals(original, migrate(original))
  }

  @Test
  fun `test migration is idempotent`() {
    val original = """implementation project(':util').sourceSets.main.output"""
    val migrated = migrate(original)

    assertEquals(migrated, migrate(migrated))
  }

  @Test
  fun `test already migrated content is left as is`() {
    val original = """
      def utilProject = rootProject.project(':util')

      configure(subprojects.findAll { it.name != 'util' }) {
          dependencies {
              testImplementation utilProject.sourceSets.main.output
          }
      }
    """.trimIndent()

    assertEquals(original, migrate(original))
  }

  @Test
  fun `test project configuration block is not touched`() {
    val original = """
      project(':util') {
          dependencies {
              implementation 'com.github.hyperskill:hs-test:release-SNAPSHOT'
          }
      }
    """.trimIndent()

    assertEquals(original, migrate(original))
  }

  @Test
  fun `test references to other modules are not touched`() {
    val original = """implementation project(':other').sourceSets.main.output"""

    assertEquals(original, migrate(original))
  }
}
