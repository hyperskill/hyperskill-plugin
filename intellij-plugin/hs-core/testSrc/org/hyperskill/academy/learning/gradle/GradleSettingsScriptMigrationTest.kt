package org.hyperskill.academy.learning.gradle

import org.junit.Assert.assertEquals
import org.junit.Test

class GradleSettingsScriptMigrationTest {

  private fun migrate(content: String): String = GradleScriptMigration.addToolchainResolver(content)

  private val hyperskillSettings = """
    buildscript {
      repositories {
        maven { url 'https://jitpack.io' }
      }

      dependencies {
        classpath "com.github.hyperskill:hs-gradle-plugin:release-SNAPSHOT"
      }
    }

    include 'util'
  """.trimIndent()

  @Test
  fun `test resolver is added right after the buildscript block`() {
    val expected = """
      buildscript {
        repositories {
          maven { url 'https://jitpack.io' }
        }

        dependencies {
          classpath "com.github.hyperskill:hs-gradle-plugin:release-SNAPSHOT"
        }
      }

      plugins {
        id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0'
      }

      include 'util'
    """.trimIndent()

    assertEquals(expected, migrate(hyperskillSettings))
  }

  @Test
  fun `test migration is idempotent`() {
    val migrated = migrate(hyperskillSettings)

    assertEquals(migrated, migrate(migrated))
  }

  @Test
  fun `test settings script of a non-hyperskill project is left as is`() {
    val original = """
      buildscript {
        repositories {
          mavenCentral()
        }
      }

      include 'util'
    """.trimIndent()

    assertEquals(original, migrate(original))
  }

  @Test
  fun `test settings script without a buildscript block is left as is`() {
    val original = """
      // hs-gradle-plugin is applied elsewhere
      include 'util'
    """.trimIndent()

    assertEquals(original, migrate(original))
  }
}
