package com.jetbrains.edu.commandLine

import kotlin.reflect.KProperty1


fun <T : EduCommand> empty(): Map<KProperty1<T, Any?>, Any?> = emptyMap()

