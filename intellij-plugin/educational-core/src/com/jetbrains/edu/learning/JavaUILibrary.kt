package com.jetbrains.edu.learning

enum class JavaUILibrary {
  SWING {
    override fun toString() = "Swing"
  },
  JCEF {
    override fun toString() = "JCEF"
  };

  @Suppress("unused", "MemberVisibilityCanBePrivate")
  companion object {
    fun isSwing(): Boolean = EduSettings.getInstance().javaUiLibrary == SWING
    fun isJCEF(): Boolean = EduSettings.getInstance().javaUiLibrary == JCEF
  }
}