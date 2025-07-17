import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
  idea
  id("common-conventions")
}

idea {
  project {
    jdkName = "17"
    languageLevel = IdeaLanguageLevel("11")
    vcs = "Git"
  }
  module {
    excludeDirs.add(file("dependencies"))
  }
}
