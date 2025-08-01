package org.hyperskill.academy.learning.taskToolWindow

import org.hyperskill.academy.learning.taskToolWindow.ui.JsEventData
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(Parameterized::class)
class JsEventDataTest(private val jsEventDataJson: String, private val expected: JsEventData) {

  @Test
  fun `test JsEventData deserialization`() {
    val jsEventData = JsEventData.fromJson(jsEventDataJson)
    assertNotNull(jsEventData)
    assertEquals(jsEventData, expected)
  }

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun testInfos(): Collection<Any> = listOf(
      arrayOf("""{"term":"abc","x":1.23,"y":2.34}""", JsEventData("abc", 1, 2, null, null)),
      arrayOf("""{"term":"abc","x":12.53,"y":32.68, "bottomOfTermRect":3.2}""", JsEventData("abc", 12, 32, 3, null)),
      arrayOf("""{"x":1.53, "topOfTermRect": 11.9, "term":"abc", "y":2.64}""", JsEventData("abc", 1, 2, null, 11)),
      arrayOf(
        """
        {
          "x":1.23,
          "y":2.64,
          "term":"abc",
          "bottomOfTermRect":6.2,
          "topOfTermRect":0.9
        }
      """.trimIndent(), JsEventData("abc", 1, 2, 6, 0)
      ),
    )
  }
}