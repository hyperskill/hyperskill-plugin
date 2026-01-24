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
| Load from server | `Load submission from server for 'Stage 1'` |

## File Categories

### Propagatable Files
- Visible files (`isVisible = true`)
- Non-test files
- Editable by learner

These files are tracked in storage and can be propagated between stages.

### Non-Propagatable Files
- Test files (in test directories or matching test patterns)
- Invisible files (`isVisible = false`)
- Files in test directories

These files are NOT stored. Test files are recreated from API cache when navigating.

### Test File Handling

Test files are handled separately:
1. Cached from API when task data is loaded (`originalTestFilesCache`)
2. Recreated on each navigation from the cache
3. Never stored in framework storage (prevents corruption across stages)

## Caches

### `originalTemplateFilesCache`
- Maps `stepId` → `Map<String, String>` (path → content)
- Contains original template files from API
- Used to calculate user changes correctly
- Populated when task files are loaded from API

### `originalTestFilesCache`
- Maps `stepId` → `Map<String, TaskFile>`
- Contains original test files from API
- Used to recreate test files on navigation
- Populated from API (anonymous request to get original files, not user submissions)

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
