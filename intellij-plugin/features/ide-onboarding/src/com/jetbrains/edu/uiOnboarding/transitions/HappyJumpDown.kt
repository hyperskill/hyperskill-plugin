package com.jetbrains.edu.uiOnboarding.transitions

import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.*
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.JUMP_DURATION
import java.awt.Point

class HappyJumpDown(data: EduUiOnboardingAnimationData, startPoint: RelativePoint, zhabaComponent: ZhabaComponent) :
  EduUiOnboardingAnimation {

  override val steps: List<EduUiOnboardingAnimationStep> = listOf(
    EduUiOnboardingAnimationStep(data.winking, startPoint, startPoint, 1_000),
    EduUiOnboardingAnimationStep(
      data.jumpDown,
      startPoint,
      pointAtTheBottom(zhabaComponent, startPoint),
      JUMP_DURATION,
      Point(0, 10),
      TransitionType.EASE_OUT
    ),
  )

  override val cycle: Boolean = false
}