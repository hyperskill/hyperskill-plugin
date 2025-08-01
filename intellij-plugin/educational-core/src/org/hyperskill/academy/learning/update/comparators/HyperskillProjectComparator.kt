package org.hyperskill.academy.learning.update.comparators

import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject

class HyperskillProjectComparator private constructor() : Comparator<HyperskillProject> {
  override fun compare(o1: HyperskillProject, o2: HyperskillProject): Int =
    compareBy(HyperskillProject::id, HyperskillProject::title, HyperskillProject::description).compare(o1, o2)

  companion object {
    infix fun HyperskillProject.isNotEqual(other: HyperskillProject): Boolean = HyperskillProjectComparator().compare(this, other) != 0
  }
}