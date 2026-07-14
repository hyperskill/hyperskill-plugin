import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
  idea
  id("common-conventions")
}

idea {
  project {
    vcs = "Git"
    jdkName = "25"
    languageLevel = IdeaLanguageLevel("25")
  }
  module {
    excludeDirs.add(file("dependencies"))
  }
}
