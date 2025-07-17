package com.jetbrains.edu.coursecreator.validation

import com.intellij.ide.actions.QualifiedNameProvider
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.IndexingTestUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import kotlinx.coroutines.runBlocking
import org.junit.Test

class CourseValidationTest : EduTestCase() {

  override fun runInDispatchThread(): Boolean = false

  override fun tearDown() {
    @Suppress("UnstableApiUsage")
    invokeAndWaitIfNeeded {
      super.tearDown()
    }
  }

  @Test
  fun `test events for check action`() {
    val course = courseWithFiles {
      lesson("lesson with unchecked tasks") {
        theoryTask("theory unchecked task") {
          checkResultFile(CheckStatus.Unchecked)
        }
        eduTask("edu unchecked task") {
          checkResultFile(CheckStatus.Unchecked)
        }
      }
      lesson("lesson with solved tasks") {
        theoryTask("theory solved task") {
          checkResultFile(CheckStatus.Solved)
        }
        eduTask("edu solved task") {
          checkResultFile(CheckStatus.Solved)
        }
        outputTask("output solved task") {
          checkResultFile("Answer")
          dir("tests") {
            taskFile("output.txt") {
              withText("Answer")
            }
          }
        }
      }
      section("section with failed tasks") {
        lesson("lesson with failed tasks") {
          theoryTask("theory failed task") {
            checkResultFile(CheckStatus.Failed)
          }
          eduTask("edu failed task") {
            checkResultFile(CheckStatus.Failed)
          }
          outputTask("output failed task") {
            checkResultFile("Answer1")
            dir("tests") {
              taskFile("output.txt") {
                withText("Answer2")
              }
            }
          }
        }
      }
    }

    doTest(
      course, validateTests = true, validateLinks = false, """
      - Test Course:
        - lesson with unchecked tasks:
          - theory unchecked task:
            - Tests: ignored
          - edu unchecked task:
            - Tests: ignored
        - lesson with solved tasks:
          - theory solved task:
            - Tests: ignored
          - edu solved task:
            - Tests: success
          - output solved task:
            - Tests: success
        - section with failed tasks:
          - lesson with failed tasks:
            - theory failed task:
              - Tests: ignored
            - edu failed task:
              - Tests: failed
            - output failed task:
              - Tests: failed
    """
    )
  }

  private fun doTest(course: Course, validateTests: Boolean, validateLinks: Boolean, expected: String) {
    IndexingTestUtil.waitUntilIndexesAreReady(project)
    val testMessageConsumer = TestServiceMessageConsumer()
    val params = ValidationParams(validateTests = validateTests, validateLinks = validateLinks)
    val validationHelper = CourseValidationHelper(params, testMessageConsumer)

    runBlocking {
      validationHelper.validate(project, course)
    }

    testMessageConsumer.assertTestTreeEquals(expected)
  }
}


private class TestXmlQualifiedNameProvider : QualifiedNameProvider {
  override fun adjustElementToCopy(element: PsiElement): PsiElement? = null
  override fun getQualifiedName(element: PsiElement): String? = null

  // It shouldn't be too precise, but it should be enough to check how we work with psi links
  override fun qualifiedNameToElement(fqn: String, project: Project): PsiElement? {
    val file = project.courseDir.findFile("config.xml")?.findPsiFile(project) as? XmlFile ?: return null
    val segments = fqn.split(".")
    var currentTag = file.rootTag?.takeIf { it.name == segments[0] } ?: return null
    for (segment in segments.drop(1)) {
      currentTag = currentTag.findFirstSubTag(segment) ?: return null
    }
    return currentTag
  }
}
