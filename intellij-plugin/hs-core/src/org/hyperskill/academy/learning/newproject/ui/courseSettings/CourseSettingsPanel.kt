package org.hyperskill.academy.learning.newproject.ui.courseSettings

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.PathUtil
import com.intellij.util.io.IOUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.capitalize
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.ext.languageDisplayName
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.EduProjectSettings
import org.hyperskill.academy.learning.newproject.HyperskillCourseAdvertiser
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CourseBindData
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CourseSelectionListener
import org.hyperskill.academy.learning.newproject.ui.coursePanel.DESCRIPTION_AND_SETTINGS_TOP_OFFSET
import org.hyperskill.academy.learning.newproject.ui.coursePanel.HORIZONTAL_MARGIN
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import java.awt.BorderLayout
import java.io.File
import java.text.DateFormat
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.DocumentListener

class CourseSettingsPanel(
  private val parentDisposable: Disposable,
  isLocationFieldNeeded: Boolean = false,
  panelTitle: String = EduCoreBundle.message("course.dialog.settings")
) : Wrapper(), CourseSelectionListener {
  var languageSettings: LanguageSettings<*>? = null
  val locationString: String?
    get() = locationField?.component?.text

  var locationField: LabeledComponent<TextFieldWithBrowseButton>? = null
  private val context: UserDataHolder = UserDataHolderBase()
  val settingsPanel = JPanel()

  private var languageSettingsDisposable: CheckedDisposable? = null

  private lateinit var collapsibleGroup: CollapsibleRow

  init {
    border = JBUI.Borders.empty(DESCRIPTION_AND_SETTINGS_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
    settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
    settingsPanel.border = JBUI.Borders.empty(0, IdeBorderFactory.TITLED_BORDER_INDENT, 5, 0)
    isOpaque = true

    val panel = panel {
      collapsibleGroup = collapsibleGroup(panelTitle) {
        row {
          cell(settingsPanel)
            .align(Align.FILL)
        }
      }.apply {
        packWindowHeight = true
      }
    }.apply {
      isOpaque = false
    }

    setContent(panel)

    if (isLocationFieldNeeded) {
      locationField = createLocationComponent()
    }
  }

  fun setSettingsComponents(settings: List<LabeledComponent<*>>) {
    settingsPanel.removeAll()
    for (setting in settings) {
      settingsPanel.add(setting, BorderLayout.PAGE_END)
    }
    UIUtil.mergeComponentsWithAnchor(settings)
    UIUtil.setBackgroundRecursively(settingsPanel, background)
    settingsPanel.revalidate()
    settingsPanel.repaint()
  }

  fun setOn(on: Boolean) {
    collapsibleGroup.expanded = on
  }

  private fun createLocationComponent(): LabeledComponent<TextFieldWithBrowseButton> {
    val field = TextFieldWithBrowseButton()
    val fileChooserDescriptor = FileChooserDescriptorFactory.singleDir()
      .withTitle(EduCoreBundle.message("action.select.course.location.title"))
      .withDescription(EduCoreBundle.message("action.select.course.location.description"))
    field.addBrowseFolderListener(null, fileChooserDescriptor)
    return LabeledComponent.create(field, EduCoreBundle.message("action.select.course.location"), BorderLayout.WEST)
  }

  fun addLocationFieldDocumentListener(listener: DocumentListener) {
    locationField?.component?.textField?.document?.addDocumentListener(listener)
  }

  fun removeLocationFieldDocumentListener(listener: DocumentListener) {
    locationField?.component?.textField?.document?.removeDocumentListener(listener)
  }

  override fun onCourseSelectionChanged(data: CourseBindData) {
    val (course, courseDisplaySettings) = data
    languageSettingsDisposable?.let(Disposer::dispose)
    val settingsDisposable = Disposer.newCheckedDisposable(parentDisposable, "languageSettingsDisposable")
    languageSettingsDisposable = settingsDisposable

    val settingsComponents = mutableListOf<LabeledComponent<*>>()
    locationField?.let {
      it.component.text = nameToLocation(course)
      settingsComponents.add(it)
    }

    val configurator = course.configurator
    languageSettings = configurator?.courseBuilder?.getLanguageSettings()?.apply {
      if (courseDisplaySettings.showLanguageSettings) {
        val components = getLanguageSettingsComponents(course, settingsDisposable, context)
        settingsComponents.addAll(components)
      }
    }

    if (settingsComponents.isNotEmpty()
        && course !is HyperskillCourseAdvertiser
        && !CoursesStorage.getInstance().hasCourse(course)
    ) {
      isVisible = true
      setSettingsComponents(settingsComponents)
    }
    else {
      isVisible = false
    }
  }

  fun getProjectSettings(): EduProjectSettings? = languageSettings?.getSettings()

  fun validateSettings(course: Course?): SettingsValidationResult {
    val settingsValidationResult = languageSettings?.validate(course, locationString) ?: SettingsValidationResult.OK
    if (settingsValidationResult is SettingsValidationResult.Ready && settingsValidationResult.validationMessage != null) {
      setOn(true)
    }

    return settingsValidationResult
  }

  companion object {
    fun nameToLocation(course: Course): String {
      val courseName = course.name
      val language = course.languageDisplayName
      val humanLanguage = course.humanLanguage
      var name = courseName
      if (!IOUtil.isAscii(name)) {
        //there are problems with venv creation for python course
        name = "${EduNames.COURSE} $language $humanLanguage".capitalize()
      }
      if (!PathUtil.isValidFileName(name)) {
        DateFormat.getDateInstance(DateFormat.DATE_FIELD, Locale.getDefault()).format(course.updateDate)
        name = FileUtil.sanitizeFileName(name)
      }
      return FileUtil.findSequentNonexistentFile(File(ProjectUtil.getBaseDir()), name, "").absolutePath
    }
  }
}
