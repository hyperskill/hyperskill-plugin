package org.hyperskill.academy.learning.newproject.ui.coursePanel.groups

import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.newproject.ui.CourseCardComponent
import org.junit.Test
import java.awt.event.MouseEvent

class CourseGroupModelTest : EduTestCase() {

  @Test
  fun `test moving mouse between cards clears previous hover without exit event`() {
    val model = CourseGroupModel()
    val selectedCard = TestCourseCard("Selected course")
    val firstHoveredCard = TestCourseCard("First hovered course")
    val secondHoveredCard = TestCourseCard("Second hovered course")
    listOf(selectedCard, firstHoveredCard, secondHoveredCard).forEach(model::addCourseCard)
    model.initialSelection()

    moveMouseOver(selectedCard)
    assertTrue(selectedCard.isHovered)

    moveMouseOver(firstHoveredCard)
    assertFalse(selectedCard.isHovered)
    assertTrue(firstHoveredCard.isHovered)

    moveMouseOver(secondHoveredCard)
    assertFalse(selectedCard.isHovered)
    assertFalse(firstHoveredCard.isHovered)
    assertTrue(secondHoveredCard.isHovered)
  }

  private fun moveMouseOver(card: CourseCardComponent) {
    val component = card.getClickComponent()
    val event = MouseEvent(component, MouseEvent.MOUSE_MOVED, 0, 0, 0, 0, 0, false)
    component.mouseMotionListeners.forEach { it.mouseMoved(event) }
  }

  private class TestCourseCard(courseName: String) : CourseCardComponent(HyperskillCourse().apply { name = courseName }) {
    var isHovered: Boolean = false
      private set

    override fun onHover(isSelected: Boolean) {
      super.onHover(isSelected)
      isHovered = true
    }

    override fun onHoverEnded() {
      isHovered = false
    }
  }
}
