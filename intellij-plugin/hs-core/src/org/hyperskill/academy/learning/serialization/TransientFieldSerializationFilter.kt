package org.hyperskill.academy.learning.serialization

import com.intellij.util.xmlb.Accessor
import com.intellij.util.xmlb.SerializationFilter
import java.lang.reflect.Modifier

/**
 * Skips accesses to fields marked with `transient` keyword (in Java) or [Transient] annotation in Kotlin
 */
object TransientFieldSerializationFilter : SerializationFilter {

  override fun accepts(accessor: Accessor, bean: Any): Boolean {
    return !isTransientFieldAccess(accessor, bean)
  }

  private fun isTransientFieldAccess(accessor: Accessor, bean: Any): Boolean {
    val field = try {
      // Do we need some cache for reflection here?
      // If so, see `com.intellij.util.xmlb.XmlSerializerImpl.XmlSerializer` as an example
      // Use getDeclaredField to find private fields, and search up the class hierarchy
      findFieldInHierarchy(bean.javaClass, accessor.name)
    }
    catch (ignore: NoSuchFieldException) {
      return false
    }

    return field != null && Modifier.isTransient(field.modifiers)
  }

  private fun findFieldInHierarchy(clazz: Class<*>?, fieldName: String): java.lang.reflect.Field? {
    var currentClass = clazz
    while (currentClass != null) {
      try {
        return currentClass.getDeclaredField(fieldName)
      }
      catch (ignore: NoSuchFieldException) {
        currentClass = currentClass.superclass
      }
    }
    return null
  }
}
