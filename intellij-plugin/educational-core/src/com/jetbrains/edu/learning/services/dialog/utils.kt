package com.jetbrains.edu.learning.services.dialog

import org.jetbrains.annotations.TestOnly

private var MOCK: ServiceHostChanger? = null

@TestOnly
fun withMockServiceHostChanger(mockUi: ServiceHostChanger, action: () -> Unit) {
  try {
    MOCK = mockUi
    action()
  }
  finally {
    MOCK = null
  }
}

