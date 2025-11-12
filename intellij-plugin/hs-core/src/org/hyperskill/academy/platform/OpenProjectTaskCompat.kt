package org.hyperskill.academy.platform

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/**
 * Compatibility helper to construct OpenProjectTask across IDE lines (252 vs 253+).
 *
 * - On newer IDEs (253+), tries to use a builder/factory if available.
 * - Otherwise, falls back to the legacy constructor with configuration lambda.
 */
object OpenProjectTaskCompat {

  /**
   * Build task for opening/creating projects with unified flags and callbacks.
   * On 253+ tries to use a nested Builder; on older IDEs falls back to legacy lambda-ctor.
   */
  @JvmStatic
  fun buildForOpen(
    forceOpenInNewFrame: Boolean,
    isNewProject: Boolean,
    isProjectCreatedWithWizard: Boolean,
    runConfigurators: Boolean,
    projectName: String?,
    projectToClose: Project?,
    beforeInit: ((Project) -> Unit)? = null,
    preparedToOpen: ((Project, Module) -> Unit)? = null
  ): OpenProjectTask {
    val built = buildWithBuilder(
      forceOpenInNewFrame,
      isNewProject,
      isProjectCreatedWithWizard,
      runConfigurators,
      projectName,
      projectToClose,
      beforeInit,
      preparedToOpen
    )
    if (built != null) return built

    // Legacy path: use lambda-ctor and configure directly on task
    return OpenProjectTask {
      this.forceOpenInNewFrame = forceOpenInNewFrame
      this.isNewProject = isNewProject
      this.isProjectCreatedWithWizard = isProjectCreatedWithWizard
      this.runConfigurators = runConfigurators
      this.projectName = projectName
      this.projectToClose = projectToClose
      if (beforeInit != null) this.beforeInit = { beforeInit(it) }
      if (preparedToOpen != null) this.preparedToOpen = { preparedToOpen(it.project, it) }
    }
  }

  /**
   * Tries to find and use a nested Builder for OpenProjectTask available in newer platform lines.
   * If anything goes wrong, returns null so the caller can fall back to legacy path.
   */
  private fun buildWithBuilder(
    forceOpenInNewFrame: Boolean,
    isNewProject: Boolean,
    isProjectCreatedWithWizard: Boolean,
    runConfigurators: Boolean,
    projectName: String?,
    projectToClose: Project?,
    beforeInit: ((Project) -> Unit)?,
    preparedToOpen: ((Project, Module) -> Unit)?
  ): OpenProjectTask? {
    return try {
      val taskClass = OpenProjectTask::class.java

      // Common pattern: OpenProjectTask has a nested class Builder with no-arg constructor and build() method
      val builderClass = taskClass.declaredClasses.firstOrNull { it.simpleName == "Builder" } ?: return null
      val builder = builderClass.getDeclaredConstructor().newInstance()

      fun callBoolean(nameVariants: Array<String>, value: Boolean) {
        for (name in nameVariants) {
          val m =
            builderClass.methods.firstOrNull { it.name == name && it.parameterCount == 1 && it.parameterTypes[0] == java.lang.Boolean.TYPE }
          if (m != null) {
            m.invoke(builder, value)
            return
          }
        }
      }

      fun callString(nameVariants: Array<String>, value: String?) {
        if (value == null) return
        for (name in nameVariants) {
          val m =
            builderClass.methods.firstOrNull { it.name == name && it.parameterCount == 1 && it.parameterTypes[0] == String::class.java }
          if (m != null) {
            m.invoke(builder, value)
            return
          }
        }
      }

      fun callProject(nameVariants: Array<String>, value: Project?) {
        if (value == null) return
        for (name in nameVariants) {
          val m =
            builderClass.methods.firstOrNull { it.name == name && it.parameterCount == 1 && Project::class.java.isAssignableFrom(it.parameterTypes[0]) }
          if (m != null) {
            m.invoke(builder, value)
            return
          }
        }
      }

      fun <T : Any> callFn1(nameVariants: Array<String>, paramClass: Class<T>, handler: (T) -> Unit) {
        for (name in nameVariants) {
          val m = builderClass.methods.firstOrNull { it.name == name && it.parameterCount == 1 } ?: continue
          val iface = m.parameterTypes[0]
          if (!iface.isInterface) continue

          // Create a dynamic proxy implementing the expected SAM interface (e.g., kotlin.jvm.functions.Function1 or Consumer)
          val proxy = java.lang.reflect.Proxy.newProxyInstance(
            iface.classLoader,
            arrayOf(iface)
          ) { _, method, args ->
            val methodName = method.name
            if (methodName == "invoke" || methodName == "accept" || methodName == "run" || methodName == "test") {
              val arg = args?.firstOrNull()
              if (arg != null && paramClass.isInstance(arg)) {
                @Suppress("UNCHECKED_CAST")
                handler(arg as T)
              }
              // For Kotlin Function1, return kotlin.Unit if available; otherwise null
              return@newProxyInstance try {
                val unitClass = Class.forName("kotlin.Unit")
                unitClass.getField("INSTANCE").get(null)
              }
              catch (_: Throwable) {
                null
              }
            }
            // Default for other Object methods
            when (methodName) {
              "toString" -> return@newProxyInstance "OpenProjectTaskCompatProxy(${iface.name})"
              "hashCode" -> return@newProxyInstance System.identityHashCode(this)
              "equals" -> return@newProxyInstance (args?.getOrNull(0) === this)
            }
            null
          }

          try {
            m.invoke(builder, proxy)
            return
          }
          catch (_: Throwable) {
            // try next name variant if invocation fails
          }
        }
      }

      // Apply flags
      callBoolean(arrayOf("forceOpenInNewFrame", "setForceOpenInNewFrame"), forceOpenInNewFrame)
      callBoolean(arrayOf("isNewProject", "setNewProject"), isNewProject)
      callBoolean(arrayOf("isProjectCreatedWithWizard", "setProjectCreatedWithWizard"), isProjectCreatedWithWizard)
      callBoolean(arrayOf("runConfigurators", "setRunConfigurators"), runConfigurators)

      // Apply simple fields
      callString(arrayOf("projectName", "setProjectName"), projectName)
      callProject(arrayOf("projectToClose", "setProjectToClose"), projectToClose)

      // Apply callbacks if supported by builder
      if (beforeInit != null) {
        callFn1(arrayOf("beforeInit", "setBeforeInit"), Project::class.java) { p -> beforeInit(p) }
      }
      if (preparedToOpen != null) {
        callFn1(arrayOf("preparedToOpen", "setPreparedToOpen"), Module::class.java) { m -> preparedToOpen(m.project, m) }
      }

      // Finalize
      val buildMethod = builderClass.methods.firstOrNull { it.name == "build" && it.parameterCount == 0 } ?: return null
      val built = buildMethod.invoke(builder)
      built as? OpenProjectTask
    }
    catch (_: Throwable) {
      null
    }
  }
}
