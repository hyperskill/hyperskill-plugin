package org.hyperskill.academy.learning.yaml.format

import com.fasterxml.jackson.databind.util.StdConverter
import org.hyperskill.academy.learning.courseFormat.StudyItem


class StudyItemConverter : StdConverter<StudyItem, String>() {
  override fun convert(item: StudyItem): String = item.name
}