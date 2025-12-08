package org.hyperskill.academy.learning.platform

import com.intellij.openapi.application.ApplicationInfo

/**
 * IDE detection utilities using public APIs.
 * Replaces internal PlatformUtils usage.
 *
 * Product codes from: https://plugins.jetbrains.com/docs/marketplace/product-codes.html
 */
object IdeDetector {
  private val currentProductCode: String by lazy {
    ApplicationInfo.getInstance().build.productCode
  }

  // IntelliJ IDEA
  fun isIntelliJ(): Boolean = currentProductCode in listOf("IC", "IU")
  fun isIdeaUltimate(): Boolean = currentProductCode == "IU"
  fun isIdeaCommunity(): Boolean = currentProductCode == "IC"

  // PyCharm
  fun isPyCharm(): Boolean = currentProductCode in listOf("PC", "PY", "DS")
  fun isPyCharmPro(): Boolean = currentProductCode == "PY"
  fun isPyCharmCommunity(): Boolean = currentProductCode == "PC"

  // Other IDEs
  fun isCLion(): Boolean = currentProductCode == "CL"
  fun isGoLand(): Boolean = currentProductCode == "GO"
  fun isPhpStorm(): Boolean = currentProductCode == "PS"
  fun isWebStorm(): Boolean = currentProductCode == "WS"
  fun isRustRover(): Boolean = currentProductCode == "RR"
  fun isRider(): Boolean = currentProductCode == "RD"
  fun isDataSpell(): Boolean = currentProductCode == "DS"
  fun isRubyMine(): Boolean = currentProductCode == "RM"
  fun isDataGrip(): Boolean = currentProductCode == "DB"
  fun isAqua(): Boolean = currentProductCode == "QA"

  // Android Studio
  fun isAndroidStudio(): Boolean = currentProductCode == "AI"

  // Educational IDEs (deprecated, but still need detection for migration)
  fun isPyCharmEducational(): Boolean = currentProductCode == "PE"
  fun isIdeaEducational(): Boolean = currentProductCode == "IE"
  fun isEducationalIde(): Boolean = currentProductCode in listOf("PE", "IE")

  /**
   * Returns the product code (e.g., "IU", "PY", "CL").
   * Use this for User-Agent strings and analytics.
   */
  fun getProductCode(): String = currentProductCode
}
