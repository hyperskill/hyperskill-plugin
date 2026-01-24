package org.hyperskill.academy.learning.framework.storage

import java.io.*
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

class FileBasedFrameworkStorage(private val baseDir: Path) : Closeable {

  private val objectsDir = baseDir.resolve("objects")
  private val refsDir = baseDir.resolve("refs")
  private val versionFile = baseDir.resolve("version")
  private val headFile = baseDir.resolve("HEAD")

  private val contentHashIndex = ConcurrentHashMap<String, String>() // SHA-256 -> SHA-256 (exists)

  init {
    Files.createDirectories(objectsDir)
    Files.createDirectories(refsDir)
    initializeContentHashIndex()
  }

  private fun initializeContentHashIndex() {
    try {
      if (Files.exists(objectsDir)) {
        Files.walk(objectsDir, 2).filter { Files.isRegularFile(it) }.forEach { path ->
          val hash = path.fileName.toString()
          contentHashIndex[hash] = hash
        }
      }
    } catch (e: Exception) {
      // Ignore or log
    }
  }

  fun getVersion(): Int {
    if (!Files.exists(versionFile)) return 0
    return try {
      Files.readAllLines(versionFile).firstOrNull()?.toInt() ?: 0
    } catch (e: Exception) {
      0
    }
  }

  fun setVersion(version: Int) {
    Files.write(versionFile, listOf(version.toString()))
  }

  // ==================== HEAD ====================

  /**
   * Get the current HEAD ref.
   * HEAD points to the current stage (like git HEAD points to current branch/commit).
   * Returns null if HEAD is not set.
   */
  fun getHead(): String? {
    if (!Files.exists(headFile)) return null
    return try {
      Files.readAllLines(headFile).firstOrNull()?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
      null
    }
  }

  /**
   * Set HEAD to point to a ref.
   * @param ref The ref to point to, or null to clear HEAD
   */
  fun setHead(ref: String?) {
    if (ref == null) {
      Files.deleteIfExists(headFile)
    } else {
      val tempFile = Files.createTempFile(baseDir, "head", ".tmp")
      try {
        Files.write(tempFile, listOf(ref))
        Files.move(tempFile, headFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
      } finally {
        Files.deleteIfExists(tempFile)
      }
    }
  }

  /**
   * Get the commit hash that HEAD points to.
   * Returns null if HEAD is not set or points to invalid ref.
   */
  fun getHeadCommit(): String? {
    val headRef = getHead() ?: return null
    return resolveRef(headRef)
  }

  /**
   * Get the snapshot that HEAD points to.
   * Returns null if HEAD is not set.
   */
  @Throws(IOException::class)
  fun getHeadSnapshot(): Map<String, String>? {
    val headRef = getHead() ?: return null
    return getSnapshot(headRef)
  }

  // ==================== Snapshots ====================

  /**
   * Get the snapshot (full state) for a ref.
   * Returns empty map if ref is null.
   */
  @Throws(IOException::class)
  fun getSnapshot(ref: String?): Map<String, String> {
    if (ref == null) return emptyMap()
    val commitHash = resolveRef(ref) ?: throw IOException("Ref $ref not found")
    return readSnapshotState(commitHash)
  }

  /**
   * Get the timestamp when snapshot was saved.
   */
  @Throws(IOException::class)
  fun getSnapshotTimestamp(ref: String?): Long {
    if (ref == null) return 0L
    val commitHash = resolveRef(ref) ?: return 0L
    return getCommit(commitHash).timestamp
  }

  /**
   * Read snapshot state from a commit or snapshot object.
   */
  private fun readSnapshotState(hash: String): Map<String, String> {
    return readObject(hash) { type, input ->
      when (type) {
        SNAPSHOT_TYPE -> {
          val snapshotMap = readSnapshotMap(input)
          snapshotMap.mapValues { (_, blobHash) -> readBlob(blobHash) }
        }
        COMMIT_TYPE -> {
          val commit = readCommit(input)
          readSnapshotState(commit.snapshotHash)
        }
        else -> throw IOException("Unknown object type: $type for hash $hash")
      }
    }
  }

  /**
   * Save a snapshot (full state) for a ref.
   *
   * @param ref The ref name (e.g., "stage_543" or "step_12345"). Must not be null.
   * @param state The file state to save (path -> content)
   * @param parentRef Optional parent ref for creating commit chain
   * @param message Optional commit message
   * @return true if a new commit was created, false if snapshot was identical to parent
   */
  @Throws(IOException::class)
  fun saveSnapshot(ref: String, state: Map<String, String>, parentRef: String? = null, message: String = ""): Boolean {
    val snapshotMap = state.mapValues { (_, text) ->
      saveBlob(text)
    }

    val snapshotHash = saveObject(SNAPSHOT_TYPE) { out ->
      FrameworkStorageUtils.writeINT(out, snapshotMap.size)
      for ((path, blobHash) in snapshotMap) {
        out.writeUTF(path)
        out.writeUTF(blobHash)
      }
    }

    // Check for existing ref first, then fall back to parent
    val existingCommitHash = resolveRef(ref)
    val parentCommitHash = existingCommitHash ?: parentRef?.let { resolveRef(it) }

    // Skip creating commit only if we're updating an EXISTING ref with identical content
    if (existingCommitHash != null && parentCommitHash != null) {
      val parentCommit = getCommit(parentCommitHash)
      if (parentCommit.snapshotHash == snapshotHash) {
        // Same content, same ref - no need to create new commit
        return false
      }
    }
    // Note: We always create a new commit for NEW refs even if snapshot is identical to parent.
    // This preserves the correct parent chain. Snapshot deduplication still saves space.

    val commitHash = saveCommit(snapshotHash, if (parentCommitHash != null) listOf(parentCommitHash) else emptyList(), message)
    updateRef(ref, commitHash)
    return true
  }

  private fun saveCommit(snapshotHash: String, parentHashes: List<String>, message: String): String {
    return saveObject(COMMIT_TYPE) { out ->
      out.writeUTF(snapshotHash)
      FrameworkStorageUtils.writeINT(out, parentHashes.size)
      for (parentHash in parentHashes) {
        out.writeUTF(parentHash)
      }
      FrameworkStorageUtils.writeLONG(out, System.currentTimeMillis())
      out.writeUTF(message)
    }
  }

  private fun readCommit(input: DataInput): Commit {
    val snapshotHash = input.readUTF()
    val parentsSize = FrameworkStorageUtils.readINT(input)
    val parentHashes = ArrayList<String>(parentsSize)
    for (i in 0 until parentsSize) {
      parentHashes.add(input.readUTF())
    }
    val timestamp = FrameworkStorageUtils.readLONG(input)
    // Read message if available (backwards compatibility with old commits)
    val message = try {
      input.readUTF()
    } catch (e: Exception) {
      ""
    }
    return Commit(snapshotHash, parentHashes, timestamp, message)
  }

  fun getCommit(hash: String): Commit {
    return readObject(hash) { type, input ->
      if (type != COMMIT_TYPE) throw IOException("Object $hash is not a commit (type=$type)")
      readCommit(input)
    }
  }

  /**
   * Resolve a ref name to its commit hash.
   */
  fun resolveRef(ref: String): String? = getRefCommitHash(ref)

  /**
   * Check if a ref exists in the storage.
   */
  fun hasRef(ref: String): Boolean = Files.exists(refsDir.resolve(ref))

  /**
   * Get all ref names in the storage.
   * Returns a list of all ref names (e.g., "stage_543", "step_12345").
   */
  fun getAllRefNames(): List<String> {
    val refs = refsDir.toFile().list() ?: return emptyList()
    return refs.filter { !it.endsWith(".tmp") }.sorted()
  }

  /**
   * Data class representing a ref with its commit information.
   */
  data class RefInfo(
    val ref: String,
    val commitHash: String,
    val commit: Commit,
    val isHead: Boolean
  )

  /**
   * Get information about all refs in the storage.
   * Returns a list of RefInfo objects with commit details.
   */
  fun getAllRefs(): List<RefInfo> {
    val headRef = getHead()
    return getAllRefNames().mapNotNull { ref ->
      val commitHash = resolveRef(ref) ?: return@mapNotNull null
      val commit = try {
        getCommit(commitHash)
      } catch (e: Exception) {
        return@mapNotNull null
      }
      RefInfo(ref, commitHash, commit, ref == headRef)
    }
  }

  data class Commit(
    val snapshotHash: String,
    val parentHashes: List<String>,
    val timestamp: Long,
    val message: String = ""
  ) {
    /**
     * Returns true if this is a merge commit (has more than one parent).
     */
    val isMerge: Boolean get() = parentHashes.size > 1
  }

  /**
   * Check if a commit is an ancestor of another commit.
   * Uses BFS to traverse the parent chain.
   *
   * @param potentialAncestor The commit hash that might be an ancestor
   * @param descendant The commit hash to check ancestry from
   * @return true if potentialAncestor is in the ancestor chain of descendant
   */
  fun isAncestor(potentialAncestor: String?, descendant: String?): Boolean {
    if (potentialAncestor == null || descendant == null) return false
    if (potentialAncestor == descendant) return true

    val visited = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue.add(descendant)

    while (queue.isNotEmpty()) {
      val current = queue.removeFirst()
      if (current == potentialAncestor) return true
      if (current in visited) continue
      visited.add(current)

      val commit = try {
        getCommit(current)
      } catch (e: Exception) {
        continue
      }

      queue.addAll(commit.parentHashes)
    }
    return false
  }

  /**
   * Save a merge snapshot with multiple parents.
   * Used when merging changes from one stage to another (Keep/Replace dialog).
   *
   * @param ref The ref name to update
   * @param state The file state to save
   * @param parentRefs List of parent refs (typically [sourceRef, targetRef] for merge)
   * @param message Commit message describing the merge
   * @return true if a new commit was created
   */
  @Throws(IOException::class)
  fun saveMergeSnapshot(ref: String, state: Map<String, String>, parentRefs: List<String>, message: String): Boolean {
    val snapshotMap = state.mapValues { (_, text) -> saveBlob(text) }

    val snapshotHash = saveObject(SNAPSHOT_TYPE) { out ->
      FrameworkStorageUtils.writeINT(out, snapshotMap.size)
      for ((path, blobHash) in snapshotMap) {
        out.writeUTF(path)
        out.writeUTF(blobHash)
      }
    }

    // Resolve all parent refs to commit hashes
    val parentCommitHashes = parentRefs.mapNotNull { resolveRef(it) }

    val commitHash = saveCommit(snapshotHash, parentCommitHashes, message)
    updateRef(ref, commitHash)
    return true
  }

  /**
   * Get snapshot state directly by snapshot hash (for debugging).
   * Reads the snapshot object and resolves all blob hashes to their content.
   */
  @Throws(IOException::class)
  fun getSnapshotByHash(snapshotHash: String): Map<String, String> {
    return readObject(snapshotHash) { type, input ->
      if (type != SNAPSHOT_TYPE) throw IOException("Object $snapshotHash is not a snapshot (type=$type)")
      val snapshotMap = readSnapshotMap(input)
      snapshotMap.mapValues { (_, blobHash) -> readBlob(blobHash) }
    }
  }

  @Throws(IOException::class)
  private fun saveBlob(text: String): String {
    val hash = hashContent(text)
    if (contentHashIndex.containsKey(hash)) return hash
    if (objectExists(hash)) {
      contentHashIndex[hash] = hash
      return hash
    }

    return saveObject(BLOB_TYPE, hash) { out ->
      out.writeUTF(hash)
      val bytes = text.toByteArray(StandardCharsets.UTF_8)
      FrameworkStorageUtils.writeINT(out, bytes.size)
      out.write(bytes)
    }
  }

  @Throws(IOException::class)
  private fun readBlob(hash: String): String {
    return readObject(hash) { type, input ->
      if (type != BLOB_TYPE) throw IOException("Object $hash is not a blob (type=$type)")
      val storedHash = input.readUTF()
      val length = FrameworkStorageUtils.readINT(input)
      if (length < 0 || length > MAX_BLOB_SIZE) {
        throw IOException("Invalid blob size $length")
      }
      val contentBytes = ByteArray(length)
      input.readFully(contentBytes)
      String(contentBytes, StandardCharsets.UTF_8)
    }
  }

  @Throws(IOException::class)
  private fun readSnapshotMap(input: DataInput): Map<String, String> {
    val size = FrameworkStorageUtils.readINT(input)
    if (size < 0 || size > MAX_SNAPSHOT_SIZE) {
      throw IOException("Invalid snapshot size $size")
    }
    val snapshot = mutableMapOf<String, String>()
    for (i in 0 until size) {
      val path = input.readUTF()
      val blobHash = input.readUTF()
      snapshot[path] = blobHash
    }
    return snapshot
  }

  /**
   * Save an object to storage with zlib compression (like git).
   * Hash is computed on uncompressed content for consistent deduplication.
   */
  fun saveObject(type: Int, preferredHash: String? = null, writer: (DataOutput) -> Unit): String {
    // Build uncompressed content
    val rawBaos = ByteArrayOutputStream()
    val rawOut = DataOutputStream(rawBaos)
    rawOut.writeByte(type)
    writer(rawOut)
    val rawBytes = rawBaos.toByteArray()

    // Hash is computed on uncompressed content (like git)
    val baseHash = preferredHash ?: hashBytes(rawBytes)
    var currentHash = baseHash
    var counter = 0

    // Compress using zlib (like git)
    val compressedBytes = compress(rawBytes)

    while (true) {
      val objectPath = getObjectPath(currentHash)
      if (!Files.exists(objectPath)) {
        Files.createDirectories(objectPath.parent)
        val tempFile = Files.createTempFile(baseDir, "obj", ".tmp")
        try {
          Files.write(tempFile, compressedBytes)
          Files.move(tempFile, objectPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } finally {
          Files.deleteIfExists(tempFile)
        }
        break
      }

      // Compare uncompressed content for deduplication
      if (contentMatchesCompressed(objectPath, rawBytes)) {
        break
      }

      counter++
      currentHash = "${baseHash}_$counter"
    }

    contentHashIndex[currentHash] = currentHash
    return currentHash
  }

  /**
   * Check if the object at path has the same content as expectedRawBytes.
   * Decompresses stored object for comparison.
   */
  private fun contentMatchesCompressed(objectPath: Path, expectedRawBytes: ByteArray): Boolean {
    return try {
      val storedBytes = Files.readAllBytes(objectPath)
      val decompressedBytes = decompress(storedBytes)
      decompressedBytes.contentEquals(expectedRawBytes)
    } catch (e: Exception) {
      // If decompression fails, try raw comparison (backwards compatibility)
      try {
        val storedBytes = Files.readAllBytes(objectPath)
        storedBytes.contentEquals(expectedRawBytes)
      } catch (e2: Exception) {
        false
      }
    }
  }

  /**
   * Read an object from storage.
   * Handles both compressed (new) and uncompressed (legacy) objects.
   */
  fun <T> readObject(hash: String, reader: (Int, DataInput) -> T): T {
    val objectPath = getObjectPath(hash)
    if (!Files.exists(objectPath)) throw IOException("Object $hash not found at $objectPath")

    val storedBytes = Files.readAllBytes(objectPath)

    // Try to decompress (new format), fall back to raw (legacy)
    val rawBytes = try {
      decompress(storedBytes)
    } catch (e: Exception) {
      // Not compressed - legacy uncompressed object
      storedBytes
    }

    DataInputStream(ByteArrayInputStream(rawBytes)).use { input ->
      val type = input.readByte().toInt()
      return reader(type, input)
    }
  }

  private fun compress(data: ByteArray): ByteArray {
    val baos = ByteArrayOutputStream()
    DeflaterOutputStream(baos).use { it.write(data) }
    return baos.toByteArray()
  }

  private fun decompress(data: ByteArray): ByteArray {
    val baos = ByteArrayOutputStream()
    InflaterInputStream(ByteArrayInputStream(data)).use { input ->
      val buffer = ByteArray(8192)
      var len: Int
      while (input.read(buffer).also { len = it } != -1) {
        baos.write(buffer, 0, len)
      }
    }
    return baos.toByteArray()
  }

  private fun updateRef(ref: String, commitHash: String) {
    val refPath = refsDir.resolve(ref)
    val tempFile = Files.createTempFile(refsDir, "ref", ".tmp")
    try {
      Files.write(tempFile, listOf(commitHash))
      Files.move(tempFile, refPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
      Files.deleteIfExists(tempFile)
    }
  }

  private fun getRefCommitHash(ref: String): String? {
    val refPath = refsDir.resolve(ref)
    if (!Files.exists(refPath)) return null
    return Files.readAllLines(refPath).firstOrNull()
  }

  private fun objectExists(hash: String): Boolean {
    return Files.exists(getObjectPath(hash))
  }

  private fun getObjectPath(hash: String): Path {
    val prefix = if (hash.length >= 2) hash.substring(0, 2) else "xx"
    return objectsDir.resolve(prefix).resolve(hash)
  }

  private fun hashContent(text: String): String {
    return hashBytes(text.toByteArray(StandardCharsets.UTF_8))
  }

  private fun hashBytes(bytes: ByteArray): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(bytes)
    return hashBytes.joinToString("") { "%02x".format(it) }
  }

  override fun close() {
    // Nothing special to close for file-based storage
  }

  fun force() {
    // Files are already synced by move
  }

  fun closeAndClean() {
    close()
    try {
      Files.walk(baseDir)
        .sorted(Comparator.reverseOrder())
        .map { it.toFile() }
        .forEach { it.delete() }
    } catch (e: IOException) {
      // Ignore
    }
  }

  companion object {
    private const val BLOB_TYPE = 1
    private const val SNAPSHOT_TYPE = 2
    private const val COMMIT_TYPE = 3

    private const val MAX_BLOB_SIZE = 100 * 1024 * 1024 // 100 MB
    private const val MAX_SNAPSHOT_SIZE = 10000
  }
}
