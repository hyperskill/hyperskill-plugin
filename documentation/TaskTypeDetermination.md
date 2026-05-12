## Task Type Determination

This document explains where task class selection happens and why a task becomes `EduTask`, `RemoteEduTask`, `CodeTask`, `TheoryTask`, or `UnsupportedTask`.

There are two main scenarios:

1. building tasks from Hyperskill/Stepik server responses
2. loading tasks from serialized local course data (JSON or YAML)

### Two different type systems

The codebase works with two related but distinct type concepts.

#### Remote step block type

This comes from Stepik/Hyperskill API and lives in `stepSource.block.name`.

Typical values:

- `pycharm`
- `code`
- `text`
- other Stepik/Hyperskill values like `choice`, `sorting`, `matching`, and so on

See:

- `intellij-plugin/hs-core/src/org/hyperskill/academy/learning/stepik/StepikSteps.kt`
- `hs-edu-format/src/org/hyperskill/academy/learning/courseFormat/hyperskill/HyperskillTaskType.kt`

#### Plugin task type

This is the internal task class identifier stored in `Task.itemType`.

Typical values:

- `edu`
- `remote_edu`
- `code`
- `theory`
- `unsupported`

These values determine which Kotlin task class is instantiated during deserialization and how checking behaves later.

### Where runtime Hyperskill tasks are determined

The main runtime decision point is:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/stepik/hyperskill/courseGeneration/HyperskillTaskBuilder.kt`

The crucial method is `build()`:

```kotlin
fun build(): Task? = when (val blockName = stepSource.block?.name) {
  EduTask.PYCHARM_TASK_TYPE if stepSource.isRemoteTested -> createTask(RemoteEduTask.REMOTE_EDU_TASK_TYPE)
  EduTask.PYCHARM_TASK_TYPE,
  "text",
  CodeTask.CODE_TASK_TYPE -> createTask(blockName)
  else -> null
}
```

This means:

- `block.name == "pycharm"` and `is_remote_tested == true` -> `RemoteEduTask`
- `block.name == "pycharm"` and `is_remote_tested == false` -> continue as plain `pycharm`
- `block.name == "code"` -> `CodeTask`
- `block.name == "text"` -> `TheoryTask`

### Why `pycharm` often becomes `EduTask`

The next step is delegation to:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/stepik/StepikTaskBuilder.kt`

`HyperskillTaskBuilder.createTask(type)` calls `super.createTask(type)`.

Inside `StepikTaskBuilder` there are two layers:

1. mapping remote Stepik/Hyperskill task names to builders
2. mapping plugin task type strings to actual task classes

Important pieces:

- `stepikTaskBuilders`
- `pluginTaskTypes`
- `pycharmTask(type: String? = null)`

For `pycharm`, the builder goes into `pycharmTask()`:

```kotlin
HyperskillTaskType.PYCHARM -> { _: String -> pycharmTask() }
```

Then:

```kotlin
val taskType = type ?: stepOptions.taskType
val task = pluginTaskTypes[taskType]?.invoke(taskName)
  ?: EduTask(taskName, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
```

This fallback is the key reason many programming tasks become `EduTask`.

If:

- no explicit override type was passed, and
- `stepOptions.taskType` is missing, or
- `stepOptions.taskType` is `pycharm`

then `pluginTaskTypes[taskType]` does not resolve to a dedicated class, and the fallback is `EduTask`.

### Why `RemoteEduTask` exists at all

`RemoteEduTask` is a special subclass of `EduTask` used when a programming task looks like a `pycharm` task structurally, but Hyperskill wants it checked remotely instead of by local tests.

That decision is made by:

- `stepSource.block.name == "pycharm"`
- `stepSource.isRemoteTested == true`

See `HyperskillStepSource.isRemoteTested` in:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/stepik/hyperskill/api/hyperskillAPI.kt`

This is the field that flips a regular programming-style task from local `EduTask` behavior into remote `RemoteEduTask` behavior.

### How `checkProfile` gets attached

Once the task instance is created, `HyperskillTaskBuilder.createTask(...)` fills Hyperskill-specific properties:

```kotlin
is EduTask -> {
  if (task is RemoteEduTask) {
    task.checkProfile = stepSource.checkProfile
  }
  name = stepSource.title
}
```

So `checkProfile` is not what decides the class. It is attached after the class has already been decided.

### CodeTask path

`CodeTask` is chosen when the server step block type is exactly `code`.

That is a separate family from `pycharm` tasks:

- `code` -> `CodeTask`
- `pycharm` -> `EduTask` or `RemoteEduTask`

This distinction matters for checking:

- `CodeTask` is remote-checked on Hyperskill
- `EduTask` is locally checked
- `RemoteEduTask` is remote-checked

### TheoryTask path

`TheoryTask` is chosen when `block.name == "text"`.

That is handled by passing `"text"` into `StepikTaskBuilder.createTask(...)`, which maps through `HyperskillTaskType.TEXT` to `pycharmTask(THEORY_TASK_TYPE)`.

### UnsupportedTask path

In generic Stepik/Hyperskill mapping, unsupported step types are represented as `UnsupportedTask`.

The fallback builder is:

```kotlin
else -> this::unsupportedTask
```

However, note that `HyperskillTaskBuilder.build()` itself currently filters accepted `block.name` values quite aggressively and may return `null` instead of building unsupported tasks in some paths. The generic `StepikTaskBuilder` still contains the broader unsupported-type behavior.

### Where serialized type is stored

Every task class defines its own `itemType`.

Examples:

- `EduTask.itemType = "edu"`
- `RemoteEduTask.itemType = "remote_edu"`
- `CodeTask.itemType = "code"`

Files:

- `hs-edu-format/src/org/hyperskill/academy/learning/courseFormat/tasks/EduTask.kt`
- `hs-edu-format/src/org/hyperskill/academy/learning/courseFormat/tasks/RemoteEduTask.kt`
- `hs-edu-format/src/org/hyperskill/academy/learning/courseFormat/tasks/CodeTask.kt`

When task metadata is serialized into Stepik-style step options, `PyCharmStepOptions.taskType` is populated from `task.itemType`:

```kotlin
taskType = task.itemType
```

See:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/stepik/StepikSteps.kt`

### JSON deserialization path

When loading local JSON, task type is determined directly from serialized `task_type`, not from `block.name`.

See:

`hs-edu-format/src/org/hyperskill/academy/learning/json/mixins/LocalEduCourseMixins.kt`

The key dispatch function is `deserializeTask(...)`.

Mapping:

- `ide` -> `IdeTask`
- `theory` -> `TheoryTask`
- `code` -> `CodeTask`
- `edu` or deprecated `pycharm` -> `EduTask`
- `output` -> `OutputTask`
- `remote_edu` -> `RemoteEduTask`
- `unsupported` -> `UnsupportedTask`

The special compatibility rule is:

```kotlin
EduTask.EDU_TASK_TYPE, EduTask.PYCHARM_TASK_TYPE -> EduTask::class.java
```

So old saved `pycharm` tasks are intentionally loaded as `EduTask`.

### YAML deserialization path

When loading YAML, task class is chosen from the `type` field.

See:

`hs-edu-format/src/org/hyperskill/academy/learning/yaml/YamlDeserializer.kt`

Mapping:

- `edu` -> `EduTask`
- `remote_edu` -> `RemoteEduTask`
- `output` -> `OutputTask`
- `theory` -> `TheoryTask`
- `ide` -> `IdeTask`
- `code` -> `CodeTask`
- `unsupported` -> `UnsupportedTask`

Again, this is direct class selection from serialized data, not inference from remote API step structure.

### Practical rules

When debugging “why is this task an `EduTask`?” use these rules:

#### If the task came from Hyperskill API:

- `block.name == "pycharm"` and `is_remote_tested == false` -> `EduTask`
- `block.name == "pycharm"` and `is_remote_tested == true` -> `RemoteEduTask`
- `block.name == "code"` -> `CodeTask`
- `block.name == "text"` -> `TheoryTask`

#### If the task came from saved JSON/YAML:

- class comes from serialized type field directly
- old `pycharm` serialized tasks are loaded as `EduTask`

### Why this split exists

Historically, `pycharm` was the generic programming-task format used by the plugin. Later, the plugin introduced internal task classes such as `EduTask` and `RemoteEduTask` with different checking semantics.

As a result:

- remote API payloads still often use `pycharm`
- internal model prefers `edu` / `remote_edu`
- deserializers keep backward compatibility with older `pycharm` task records

That is why task type determination can look inconsistent until you separate:

1. server block type
2. internal task class type
3. serialized local task type
