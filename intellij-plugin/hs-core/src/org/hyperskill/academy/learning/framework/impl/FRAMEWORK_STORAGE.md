# Framework Storage Architecture

This document describes the architecture and logic of the Framework Lesson storage system, which tracks user changes across stages in framework lessons.

## Overview

Framework lessons consist of multiple stages (tasks) that share the same project directory. As users navigate between stages, their changes need to be preserved and optionally propagated to subsequent stages. The storage system uses a **git-like content-addressable approach** with snapshots, commits, and refs.

## Core Concepts

### Storage Structure

```
.idea/frameworkLessonHistory/storage_v3/
├── HEAD                    # Current stage ref (e.g., "stage_548")
├── objects/               # Content-addressable blob storage
│   ├── 3c/
│   │   └── 48ada0c56e7228...  # Blob files (SHA-256 hash)
│   └── ...
├── commits/               # Commit metadata (JSON)
│   ├── abc123...json
│   └── ...
└── refs/                  # Stage references
    ├── stage_543          # Points to commit hash
    ├── stage_544
    └── ...
```

### Refs (References)

Each stage has a ref named `stage_<stepId>` (or `step_<stepId>` for step-based tasks). The ref file contains a commit hash pointing to the latest snapshot for that stage.

### Commits

A commit contains:
- **snapshot**: Map of file paths to content hashes
- **parent**: Parent commit hash (for history chain)
- **message**: Human-readable description
- **timestamp**: When the commit was created

### Snapshots

A snapshot is a complete state of all propagatable files (visible, non-test files) for a stage. Unlike git diffs, we store **full file contents** because:
1. After IDE restart, templates may not be available
2. Diffs require correct templates to apply
3. Full snapshots are self-contained

## Navigation Flow

### Forward Navigation (e.g., Stage 1 → Stage 5)

```
┌─────────────────────────────────────────────────────────────────┐
│                    Forward Navigation Flow                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. User is on Stage 1, makes changes                           │
│  2. User clicks "Next" or jumps to Stage 5                      │
│                                                                  │
│  For each intermediate stage (1→2, 2→3, 3→4, 4→5):             │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Save current stage snapshot                               │   │
│  │ Message: "Save changes before navigating from X to Y"     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                           │                                      │
│                           ▼                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Check: userMadeNewChanges OR propagationActive?           │   │
│  └──────────────────────────────────────────────────────────┘   │
│            │                              │                      │
│     YES    │                              │  NO                  │
│            ▼                              ▼                      │
│  ┌─────────────────────┐      ┌─────────────────────────────┐   │
│  │ Show Keep/Replace   │      │ Just navigate               │   │
│  │ dialog              │      │ (no dialog)                 │   │
│  └─────────────────────┘      └─────────────────────────────┘   │
│       │           │                                              │
│  KEEP │           │ REPLACE                                      │
│       ▼           ▼                                              │
│  ┌─────────┐  ┌──────────────────────────────────────────────┐  │
│  │ Stop    │  │ propagationActive = true                     │  │
│  │ propag. │  │ Apply current changes to target              │  │
│  └─────────┘  │ Continue to next intermediate stage          │  │
│               └──────────────────────────────────────────────┘  │
│                           │                                      │
│                           ▼                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Save target stage snapshot                                │   │
│  │ Message (if propagationActive):                           │   │
│  │   "Propagate changes to 'X' (user chose Replace)"         │   │
│  │ Message (otherwise):                                      │   │
│  │   "Navigate to 'X'"                                       │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Backward Navigation (e.g., Stage 5 → Stage 3)

When navigating backward:
1. **No snapshot is saved** for the current stage (disk content belongs to a different stage)
2. `propagationActive` is reset to `false`
3. Target stage content is restored from storage
4. Test files are recreated from API cache

### Merge Logic (Git-like)

The system uses git-like ancestor checking to determine if a merge is needed:

1. **Check ancestor relationship**: Is the current stage's commit an ancestor of the target stage's commit?
2. **If YES (ancestor)**: No merge needed - changes were already propagated. Just navigate.
3. **If NO (not ancestor)**: Merge needed - show Keep/Replace dialog.

### Keep vs Replace Dialog

When navigating forward and merge is needed (current commit is NOT an ancestor of target commit):

| Choice | Behavior |
|--------|----------|
| **Keep** | Create merge commit with target's content. Parents: `[targetRef, currentRef]`. Stop propagation. Like `git merge --strategy=ours`. |
| **Replace** | Create merge commit with current's content. Parents: `[targetRef, currentRef]`. Continue propagation. Like `git merge --strategy=theirs`. |

### Merge Commits

Merge commits have **two parents**:
- First parent: target stage's previous commit (what we're merging INTO)
- Second parent: current stage's commit (what we're merging FROM)

After a merge commit is created, the current stage's commit becomes an ancestor of the target stage. This means:
- Future navigations from current to target won't show the dialog (ancestor check passes)
- The merge decision is recorded in the commit history

```
Before merge (stage_543 not ancestor of stage_544):
stage_543: A' ← A ← null
stage_544: B ← A ← null

After Replace merge:
stage_543: A' ← A ← null
stage_544: M ← [B, A'] (merge commit with content from A')
                ↑
            A' is now an ancestor of M
```

## Auto-Save

The system auto-saves the current stage snapshot when:
- User presses Ctrl+S (save all documents)
- IDE is about to close
- Any document save event occurs

**Important**: Auto-save is disabled during navigation (`isNavigating` flag) to prevent creating commits with "Auto-save" messages when navigation code should be creating commits with proper messages.

```kotlin
private fun saveCurrentTaskSnapshot() {
    // Skip auto-save during navigation
    if (isNavigating) return

    // ... save logic with message "Auto-save changes for 'Stage X'"
}
```

## Commit Messages

| Scenario | Message |
|----------|---------|
| Saving before navigation | `Save changes before navigating from 'Stage 1' to 'Stage 2'` |
| Merge - Keep | `Merge from 'Stage 1': Keep target changes` |
| Merge - Replace | `Merge from 'Stage 1': Replace with propagated changes` |
| Normal navigation | `Navigate to 'Stage 2'` |
| Auto-save | `Auto-save changes for 'Stage 1'` |
| Load from server | `Load submission from server for 'Stage 1' (submission #12345)` |

## File Categories and Propagation Rules

### Overview

File propagation determines what happens when a user navigates between stages in a non-template-based framework lesson. The rules are aligned with the server-side logic.

### Propagation Mode

Controlled by `FrameworkLesson.isTemplateBased`:

| Mode | `isTemplateBased` | Behavior |
|------|-------------------|----------|
| **Template-based** | `true` | No propagation. Each stage uses author's original files. |
| **Non-template-based** | `false` | User's visible files propagate to subsequent stages. |

On the server, this is checked via `Step.block_is_inherited`:
```python
@property
def block_is_inherited(self) -> bool:
    if self.block_name != BlockName.PYCHARM:
        return False
    project = self.get_project()
    return bool(project and not project.is_template_based)
```

### Propagatable Files

**Definition**: `TaskFile.shouldBePropagated() = isVisible && isEditable`

These are user-editable files that transfer between stages:
- Visible to user (`isVisible = true`)
- Editable by user (`isEditable = true`)
- NOT learner-created (`isLearnerCreated = false` for storage purposes)

These files are:
- Tracked in storage snapshots
- Propagated forward when user navigates (in non-template-based mode)
- Merged using Keep/Replace dialog when conflicts exist

**Server-side equivalent**:
```python
# In build_block():
for solution_file in solution:
    if not solution_file['is_visible']:
        continue  # Only visible files propagate
    # ... copy file to next stage
```

### Non-Propagatable Files

**Definition**: `!TaskFile.shouldBePropagated()` = `!isVisible || !isEditable`

Two categories:

1. **Test files** (typically `!isVisible`):
   - Files in test directories (e.g., `test/`, `tests/`)
   - Hidden from user but executed during check
   - Each stage has its own test files from author

2. **Hidden/Read-only files** (`!isEditable` or `!isVisible`):
   - Configuration files user shouldn't modify
   - Reference implementations
   - Any file author marked as non-editable

These files are:
- NOT stored in snapshots (would corrupt stage-specific content)
- Cached separately in `originalNonPropagatableFilesCache`
- Recreated from API/cache on each navigation
- Each stage gets fresh files from author, not from previous stage

**Server-side equivalent**:
```python
# In build_block():
files.extend(
    block_file
    for block_file in block['options']['files']
    if block_file.get('name') not in user_files  # Add author's new files
)
```
Server adds author's files that weren't in user's solution (test, hidden, new files).

### File Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Navigation: Stage N → Stage N+1                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  PROPAGATABLE FILES (isVisible && isEditable):                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ Stage N disk → Storage snapshot → Apply to Stage N+1 disk        │   │
│  │ (User's Main.kt, utils.py, etc.)                                 │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  NON-PROPAGATABLE FILES (!isVisible || !isEditable):                    │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │ API/Cache for Stage N+1 → Recreate on disk                       │   │
│  │ (Test files, hidden configs - fresh for each stage)              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### Storage Snapshot Contents

A full snapshot for a stage contains:
1. **User files** (from disk): All propagatable files user has on disk
2. **Non-propagatable files** (from cache): Test and hidden files from API

```kotlin
fun buildFullSnapshotState(task: Task): Map<String, String> {
    val result = mutableMapOf<String, String>()

    // 1. Read propagatable files from disk
    for ((path, taskFile) in task.taskFiles) {
        if (taskFile.shouldBePropagated()) {
            result[path] = readFromDisk(path)
        }
    }

    // 2. Add non-propagatable files from cache (NOT from disk!)
    val cached = originalNonPropagatableFilesCache[task.id]
    for ((path, taskFile) in cached) {
        result[path] = taskFile.contents
    }

    return result
}
```

### Why Non-Propagatable Files Are Cached Separately

1. **Stage isolation**: Each stage has its own test files; propagating them would break subsequent stages
2. **Offline support**: After IDE restart, API may not be available; cache preserves test files
3. **Author updates**: When author updates test files, we update cache and storage snapshot
4. **Corruption prevention**: TaskFile.contents may get overwritten with wrong stage's content during navigation

### Cache Priority for Non-Propagatable Files

When loading non-propagatable files:
1. **Cache** (`originalNonPropagatableFilesCache`) - preferred, contains API data
2. **API** (if cache miss) - fetch fresh from server
3. **task.taskFiles** (last resort) - may be stale/corrupted

## Caches

### `originalTemplateFilesCache`
- Maps `stepId` → `Map<String, String>` (path → content)
- Contains original template files (propagatable files) from API
- Used to calculate user changes correctly in `saveExternalChanges()`
- Populated when task files are loaded from API
- Used for: calculating diff between user's code and original template

### `originalNonPropagatableFilesCache`
- Maps `stepId` → `Map<String, TaskFile>` (path → TaskFile with metadata)
- Contains all non-propagatable files: test files AND hidden/read-only files
- Filter: `!isVisible || !isEditable` (opposite of `shouldBePropagated()`)
- Used to recreate non-propagatable files on navigation
- Populated from API (anonymous request to get original files, not user submissions)
- Preserves `isEditable` property per file (some hidden files may be editable, some not)

## Migration from Legacy Storage

The old storage used auto-incrementing integer IDs (`task.record`). The new storage uses string-based refs (`stage_<stepId>`).

Migration happens automatically:
1. When loading submissions, legacy changes are applied on top of API state
2. `task.record` is cleared after migration
3. Legacy storage data remains for fallback but is not used for new operations

## Error Handling

### Storage Corruption
If storage data is corrupted:
1. Error is logged
2. Storage is recreated (files deleted, refs reset)
3. All `task.record` values are reset to `-1`

### API Failures
If API fails to load template/test files:
1. Fall back to `task.taskFiles` (less reliable)
2. Log warning
3. Continue with available data

## Thread Safety

- `isNavigating` flag prevents concurrent auto-save during navigation
- `originalTemplateFilesCache` and `originalTestFilesCache` use `ConcurrentHashMap`
- Storage operations are synchronized where needed

## Key Implementation Details

### `applyTargetTaskChanges` Flow

1. Set `isNavigating = true` (wrapped in try-finally)
2. Load templates from API if cache empty
3. Save current stage snapshot (forward navigation only)
4. Calculate propagation changes or simple diff
5. Apply changes to disk
6. Recreate test files
7. Save target stage snapshot
8. Update HEAD
9. Set `isNavigating = false` (in finally block)

### `propagationActive` Flag

- Set to `true` when user chooses Replace
- Set to `false` when user chooses Keep or navigates backward
- Controls whether dialog is shown for subsequent stages during jump
- Also controls commit message text

## Debugging

To inspect storage state:

```bash
# View HEAD (current stage)
cat .idea/frameworkLessonHistory/storage_v3/HEAD

# View ref for a stage
cat .idea/frameworkLessonHistory/storage_v3/refs/stage_543

# View commit metadata
cat .idea/frameworkLessonHistory/storage_v3/commits/<hash>.json

# List all commits
ls -la .idea/frameworkLessonHistory/storage_v3/commits/

# Count objects
ls .idea/frameworkLessonHistory/storage_v3/objects/*/ | wc -l
```

Logs are written to IDE log with prefix `FrameworkLessonManagerImpl`:
```
tail -f build/idea-sandbox/idea-sandbox-*/log_runIdea/idea.log | grep -i framework
```
