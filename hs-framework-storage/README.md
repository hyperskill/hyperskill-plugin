# hs-framework-storage

Git-like file-based storage for Framework Lesson user changes in Hyperskill Academy plugin.

## Overview

This module provides a standalone storage system for tracking user modifications in Framework Lessons. Framework Lessons allow students to work through multi-stage projects where each stage builds upon the previous one. The storage tracks what files the user has modified, added, or deleted relative to the original template.

## Why Git-like Snapshots?

### Problem with the Old Diff-Based Approach

The previous storage format stored **diffs** (change lists) instead of full state:

```kotlin
// Old format: list of changes relative to template
UserChanges(listOf(
    AddFile("helper.kt", "..."),      // User created new file
    ChangeFile("main.kt", "..."),     // User modified template file
    RemoveFile("example.kt")          // User deleted template file
))
```

**Problems:**

1. **Template dependency**: To reconstruct user's state, you need the original template files:
   ```
   current_state = template_files + apply(changes)
   ```
   But templates may not be available after IDE restart (cache cleared, API fails).

2. **Fragile restoration**: If templates change on the server, applying old diffs produces wrong results.

3. **Complex change type logic**: Code had to track whether a file was "added" vs "changed" vs "deleted", updating types when templates change.

4. **IDE restart issues (ALT-10961)**: After restart, plugin couldn't correctly restore user's work because template cache was empty.

### Git's Solution: Store Full Snapshots

Git doesn't store diffs in commits — it stores **complete snapshots** (trees):

```
Commit A: tree = {main.kt, helper.kt, test.kt}
Commit B: tree = {main.kt, helper.kt}  ← test.kt simply absent
```

Diffs are computed **on-the-fly** when needed by comparing two snapshots:
- File in A but not in B → deleted
- File in B but not in A → added
- File in both but different → modified

### New Approach

```kotlin
// New format: complete state snapshot
Snapshot(mapOf(
    "main.kt" to "user's version...",
    "helper.kt" to "user created..."
    // test.kt not present = user deleted it
))
```

**Benefits:**

1. **Self-contained**: Snapshot has everything needed to restore user's state. No template dependency.

2. **Robust to template changes**: User's work is preserved exactly as they left it.

3. **Simpler logic**: No change type tracking. Diff calculated when needed:
   ```kotlin
   fun calculateChanges(template: Map<String, String>, snapshot: Map<String, String>): UserChanges
   ```

4. **Reliable after restart**: Snapshot is the source of truth, not template + diffs.

## Architecture

The storage uses a git-inspired content-addressable design:

```
storage_v3/
├── objects/           # Content-addressable object store (like .git/objects/)
│   └── ab/
│       └── abc123...  # Objects stored by SHA-256 hash prefix
├── refs/              # References: task ID -> commit hash (like .git/refs/)
│   ├── 1              # Ref for Task.record=1
│   └── 2              # Ref for Task.record=2
├── HEAD               # Points to current stage's ref ID (like .git/HEAD)
└── version            # Storage format version
```

### HEAD

Like git's HEAD, our HEAD file points to the current stage. It contains a ref ID (integer), not a commit hash directly.

```kotlin
// Get current stage
val currentRefId = storage.getHead()  // Returns -1 if not set

// Set HEAD when navigating to a stage
storage.setHead(task.record)

// Get current stage's snapshot directly
val snapshot = storage.getHeadSnapshot()

// Get current stage's commit hash
val commitHash = storage.getHeadCommit()
```

HEAD is updated automatically when navigating between stages in a Framework Lesson.

### Object Types

| Type | ID | Description |
|------|-----|-------------|
| BLOB | 1 | File content with SHA-256 hash |
| SNAPSHOT | 2 | Map of file paths to blob hashes (like git tree) |
| COMMIT | 3 | Snapshot hash + parent commits + timestamp |

### Compression

Like git, all objects are compressed using **zlib** (Deflate algorithm) before writing to disk. This significantly reduces storage size for text-based source files.

- Hash is computed on **uncompressed** content (for consistent deduplication)
- Objects are stored **compressed** on disk
- Reading automatically decompresses

### Data Flow

```
User modifies file
       ↓
saveSnapshot(refId, state)
       ↓
┌──────────────────────────────┐
│ For each file in state:      │
│   1. Compute SHA-256 hash    │
│   2. Check if blob exists    │
│   3. If not, save new blob   │
└──────────────────────────────┘
       ↓
Create SNAPSHOT object (path → blob hash)
       ↓
Create COMMIT object (snapshot + parents + timestamp)
       ↓
Update ref to point to new commit
```

## Key Classes

### FileBasedFrameworkStorage

Main storage implementation. Thread-safe, supports concurrent access.

```kotlin
val storage = FileBasedFrameworkStorage(basePath)

// Save complete state snapshot
val refId = storage.saveSnapshot(
    refId = -1,           // -1 for new ref, or existing ID
    state = mapOf(
        "src/Main.kt" to "fun main() { ... }",
        "test/Test.kt" to "class Test { ... }"
    ),
    parentRefId = previousRefId  // Optional parent for history
)

// Retrieve state as UserChanges
val changes = storage.getUserChanges(refId)

// Access commit history (refs work like git refs - they point to commits)
val commitHash = storage.resolveRef(refId)
val commit = storage.getCommit(commitHash!!)
println("Parents: ${commit.parentHashes}")
println("Timestamp: ${commit.timestamp}")
```

### UserChanges

Represents a list of file modifications:

```kotlin
sealed class Change {
    class AddFile(path: String, text: String)
    class RemoveFile(path: String)
    class ChangeFile(path: String, text: String)
    class PropagateLearnerCreatedTaskFile(path: String, text: String)
    class RemoveTaskFile(path: String)
}

class UserChanges(
    val changes: List<Change>,
    val timestamp: Long
)
```

### FrameworkStorageUtils

VLQ (Variable-Length Quantity) encoding for compact integer storage:

```kotlin
// Efficient encoding: small values = fewer bytes
// 0-191: 1 byte
// 192-12479: 2 bytes
// etc.
FrameworkStorageUtils.writeINT(output, value)
val value = FrameworkStorageUtils.readINT(input)
```

## Features

### Content Deduplication

Files with identical content share the same blob:

```kotlin
// Both files have same content "hello"
storage.saveSnapshot(-1, mapOf("a.txt" to "hello"))
storage.saveSnapshot(-1, mapOf("b.txt" to "hello"))
// Only ONE blob is stored, referenced by both snapshots
```

### Commit History

Each save creates a commit. Refs (like git branches) point to commits:

```kotlin
val id1 = storage.saveSnapshot(-1, state1)
val id2 = storage.saveSnapshot(id1, state2)  // id1's commit becomes parent

val commit2 = storage.getCommit(storage.resolveRef(id2)!!)
assert(commit2.parentHashes.contains(storage.resolveRef(id1)))
```

### Cross-Ref History

Different refs can share commit history:

```kotlin
val id1 = storage.saveSnapshot(-1, state1)
// Create new ref with id1's commit as parent
val id2 = storage.saveSnapshot(-1, state2, parentRecord = id1)
```

### Collision Handling

Hash collisions are handled by appending counter:

```
objects/ab/abc123        # First object with this hash
objects/ab/abc123_1      # Second object (different content, same hash)
```

## Binary Format

### VLQ Integer Encoding

```
Value 0-191:     [value]
Value 192+:      [192 + (value & 0x3F)] [recursive(value >> 6)]
```

### Object Format

```
[type: 1 byte]
[type-specific data...]
```

#### BLOB (type=1)
```
[type=1] [hash: UTF] [length: VLQ INT] [content: bytes]
```

#### SNAPSHOT (type=2)
```
[type=2] [count: VLQ INT] [path: UTF, blobHash: UTF]...
```

#### COMMIT (type=3)
```
[type=3] [snapshotHash: UTF] [parentCount: VLQ INT] [parentHash: UTF]... [timestamp: VLQ LONG]
```

## Migration from Legacy Storage

The plugin previously used IntelliJ's `AbstractStorage` (binary format). Migration happens automatically:

1. Legacy storage detected at `.idea/frameworkLessonHistory/storage` (file)
2. Data read using `DataInputOutputUtil` (IntelliJ VLQ format)
3. Written to new format at `.idea/frameworkLessonHistory/storage_v3/` (directory)
4. Legacy files kept for debugging

## Dependencies

This module has minimal dependencies:
- `kotlin-stdlib` (compile-only, provided by IDE at runtime)
- `annotations` (compile-only)

No IntelliJ Platform dependencies - can be used standalone.

## Testing

```bash
./gradlew :hs-framework-storage:test
```

Tests cover:
- Basic save/load operations
- Content deduplication
- Commit history
- Collision resolution
- Cross-ref relationships
