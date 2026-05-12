## CheckAction

This document explains how `org.hyperskill.academy.learning.actions.CheckAction` works, what code paths it executes, and how local and remote checking are combined.

### Entry point

The action is registered in `intellij-plugin/hs-core/resources/META-INF/hs-core.xml`:

```xml
<action id="HyperskillEducational.Check" class="org.hyperskill.academy.learning.actions.CheckAction"/>
```

The implementation is in `intellij-plugin/hs-core/src/org/hyperskill/academy/learning/actions/CheckAction.kt`.

### High-level flow

`CheckAction.actionPerformed(...)` does the following:

1. Retrieves `Project` from the action event.
2. Refuses to run while indexing is active (`DumbService.isDumb(project)`).
3. Clears the check details view.
4. Saves all open documents.
5. Reads the current task from `TaskToolWindowView`.
6. Acquires a per-project lock so only one check can run at a time.
7. Calls all registered `CheckListener.beforeCheck(...)`.
8. Starts a background task `StudyCheckTask`.

Relevant code:

- `CheckAction.kt`, `actionPerformed`
- `CheckAction.kt`, `CheckActionState`

### Core execution graph

```text
CheckAction.actionPerformed
  -> CheckListener.beforeCheck(project, task)
  -> StudyCheckTask.run(indicator)
     -> localCheck(indicator)
        -> recreateTestFiles(taskDir)
        -> maybe createTests(invisibleTestFiles)
        -> checker.check(indicator)
     -> if local result is Failed: stop
     -> remoteCheckerForTask(project, task)
     -> remoteChecker?.check(project, task, indicator) ?: localResult
  -> onSuccess()
     -> update task.status
     -> update task.feedback
     -> saveItem(task)
     -> checker.onTaskSolved() / checker.onTaskFailed()
     -> TaskToolWindowView.checkFinished(...)
     -> CheckListener.afterCheck(project, task, result)
```

### How the local checker is chosen

`StudyCheckTask` creates a local checker through the course configurator:

```kotlin
val configurator = task.course.configurator
checker = configurator?.taskCheckerProvider?.getTaskChecker(task, project)
```

The generic selection logic is in `intellij-plugin/hs-core/src/org/hyperskill/academy/learning/checker/TaskCheckerProvider.kt`.

Important behavior:

- `RemoteEduTask` -> no local checker
- `CodeTask` -> no local checker
- `TheoryTask` -> no local checker
- `UnsupportedTask` -> no local checker
- `EduTask` -> configurator-specific local checker
- `OutputTask` -> `OutputTaskChecker`
- `IdeTask` -> `IdeTaskChecker`

This is why not every task goes through local tests.

### What `localCheck(...)` does

`StudyCheckTask.localCheck(...)` performs the local phase:

1. If no checker exists, returns `CheckResult.NO_LOCAL_CHECK`.
2. Resolves the task directory.
3. Recreates test files from task metadata before running checks.
4. For non-Hyperskill courses, restores invisible test files into the project tree.
5. Calls `checker.check(indicator)`.

Relevant methods:

- `CheckAction.kt`, `localCheck`
- `CheckAction.kt`, `recreateTestFiles`
- `CheckAction.kt`, `createTests`

### Why test files are recreated

Before running a local check, the plugin restores author-provided test files from the task model. This prevents a learner from changing, deleting, or corrupting tests to fake a successful check.

For framework lessons, the plugin uses `FrameworkLessonManager` cached original test files rather than the current `task.taskFiles`, because framework tasks may otherwise carry stale test data from another stage.

### How local test execution works

The standard base implementation for local `EduTask` checking is `EduTaskCheckerBase` in:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/checker/EduTaskCheckerBase.kt`

Its `check(...)` method:

1. Hides the Run tool window.
2. Calls `EnvironmentChecker.getEnvironmentError(project, task)`.
3. Builds run configurations.
4. Validates each configuration.
5. Executes them using IntelliJ run infrastructure.
6. Collects test results.
7. Produces a `CheckResult`.

If execution starts but tests do not actually run, subclasses may translate stderr into a more specific error result.

### How run configurations are built and executed

The utility layer is in:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/checker/CheckUtils.kt`

Key pieces:

- `getCustomRunConfigurationForChecker(...)`
- `createDefaultRunConfiguration(...)`
- `executeRunConfigurations(...)`

Execution details:

- A custom task-specific run configuration in `.idea/runConfigurations` is preferred if present.
- Otherwise, IntelliJ derives temporary run configurations from PSI context.
- Configurations run through `ProgramRunner` and `ExecutionEnvironmentBuilder`.
- The plugin tracks all started environments and waits on a `CountDownLatch`.
- Test results are collected through a `TestResultCollector`.

### Local-vs-remote ordering

`StudyCheckTask.run(...)` always does the local phase first:

```kotlin
val localCheckResult = localCheck(indicator)
if (localCheckResult.status === CheckStatus.Failed) {
  result = localCheckResult
  return
}
val remoteChecker = remoteCheckerForTask(project, task)
result = remoteChecker?.check(project, task, indicator) ?: localCheckResult
```

This means:

- A local `Failed` result stops the pipeline immediately.
- Remote checking is attempted only if local checking did not fail.
- If there is no remote checker, the final result is the local result.

### How the remote checker is chosen

Remote checkers are selected through the extension point:

`HyperskillEducational.remoteTaskChecker`

The manager is:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/checker/remote/RemoteTaskCheckerManager.kt`

It:

1. Collects all registered remote checkers.
2. Filters them by `canCheck(project, task)`.
3. Returns exactly one checker or `null`.
4. Throws if more than one checker matches.

### Hyperskill remote checker

Hyperskill registers:

```xml
<remoteTaskChecker implementation="org.hyperskill.academy.learning.stepik.hyperskill.checker.HyperskillRemoteTaskChecker"/>
```

Implementation:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/stepik/hyperskill/checker/HyperskillRemoteTaskChecker.kt`

It can check only when:

- `task.course is HyperskillCourse`
- `HyperskillCheckConnector.isRemotelyChecked(task)` is true

That remote set is:

- `CodeTask`
- `RemoteEduTask`
- `UnsupportedTask`

### Hyperskill remote task behavior by type

#### CodeTask

Path:

- `HyperskillRemoteTaskChecker.check(...)`
- `HyperskillCheckConnector.checkCodeTask(...)`

Behavior:

1. Validates that task id exists.
2. Tries websocket-based check session.
3. If websocket path fails, falls back to HTTP submission.
4. Polls the submission until status changes from `evaluation`.

#### RemoteEduTask

Path:

- `HyperskillRemoteTaskChecker.check(...)`
- `HyperskillCheckConnector.checkRemoteEduTask(...)`

Behavior:

1. Validates that task id exists.
2. Collects solution files.
3. Creates an attempt.
4. Creates and posts a submission.
5. Polls until final status is received.

#### UnsupportedTask

Path:

- `HyperskillRemoteTaskChecker.check(...)`
- `HyperskillCheckConnector.checkUnsupportedTask(...)`

Behavior:

1. Does not create a new submission.
2. Loads existing submissions from the platform.
3. Derives solved/failed state from the latest known submissions.

### Hyperskill local EduTask behavior

Hyperskill `EduTask` is special:

- It is checked locally.
- It does not use the remote checker.
- After the local result is finalized, `HyperskillCheckListener.afterCheck(...)` may asynchronously post the solution to Hyperskill.

This listener is registered in `META-INF/Hyperskill.xml`.

Implementation:

`intellij-plugin/hs-core/src/org/hyperskill/academy/learning/stepik/hyperskill/checker/HyperskillCheckListener.kt`

Behavior:

- `beforeCheck(...)` updates Hyperskill metrics.
- `afterCheck(...)` restarts metrics for unsolved current tasks.
- For non-remote, non-theory Hyperskill tasks, it posts the local solution to Hyperskill in background if the user is logged in.

This postback is not the same thing as remote checking. It is a follow-up side effect after local checking has already completed.

### Result finalization

When background execution succeeds, `StudyCheckTask.onSuccess()`:

1. Stores `task.status = checkResult.status`
2. Stores `task.feedback`
3. Persists the task via `saveItem(task)`
4. Calls checker lifecycle hooks
5. Updates the tool window
6. Refreshes course progress and project view
7. Calls all `CheckListener.afterCheck(...)`

### Error and cancel behavior

If the background task is cancelled:

- the tool window is reset to `readyToCheck()`

If an exception happens:

- refresh-token failure is converted to `failedToSubmit(...)`
- everything else becomes generic `failedToCheck`

### Practical summary

For the common Hyperskill task classes:

- `EduTask`: local tests first, then optional async postback to Hyperskill
- `RemoteEduTask`: remote-only check
- `CodeTask`: remote-only check
- `UnsupportedTask`: remote state lookup only, no fresh submission

This local-first and then maybe-remote pattern is the most important rule to keep in mind when debugging `CheckAction`.
