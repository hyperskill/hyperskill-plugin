#!/usr/bin/env python3
"""
Check Framework Storage state for a Hyperskill project.
Usage: python3 check_storage.py /path/to/project
"""

import zlib
import struct
import os
import sys
from datetime import datetime

def read_varint(data, pos):
    b = data[pos]
    if b < 192:
        return b, pos + 1
    else:
        rest, new_pos = read_varint(data, pos + 1)
        return (b - 192) + (rest << 6), new_pos

def read_varlong(data, pos):
    b = data[pos]
    if b < 192:
        return b, pos + 1
    else:
        rest, new_pos = read_varlong(data, pos + 1)
        return (b - 192) + (rest << 6), new_pos

def read_utf(data, pos):
    length = struct.unpack('>H', data[pos:pos+2])[0]
    s = data[pos+2:pos+2+length].decode('utf-8', errors='replace')
    return s, pos + 2 + length

def read_object(storage_path, hash_val):
    prefix = hash_val[:2]
    path = f"{storage_path}/objects/{prefix}/{hash_val}"
    with open(path, "rb") as f:
        compressed = f.read()
    return zlib.decompress(compressed)

def parse_commit(data):
    pos = 0
    obj_type = data[pos]
    pos += 1
    if obj_type != 3:  # COMMIT_TYPE = 3
        return None
    snapshot_hash, pos = read_utf(data, pos)
    parents_size, pos = read_varint(data, pos)
    parent_hashes = []
    for _ in range(parents_size):
        parent_hash, pos = read_utf(data, pos)
        parent_hashes.append(parent_hash)
    timestamp, pos = read_varlong(data, pos)
    try:
        message, pos = read_utf(data, pos)
    except:
        message = ""
    return {
        'snapshot_hash': snapshot_hash,
        'parent_hashes': parent_hashes,
        'timestamp': timestamp,
        'message': message,
        'is_merge': len(parent_hashes) > 1
    }

def check_storage(project_path, verbose=False):
    storage_path = os.path.join(project_path, ".idea/frameworkLessonHistory/storage_v3")

    if not os.path.exists(storage_path):
        print(f"Storage not found at: {storage_path}")
        return

    # Read HEAD
    head = "(none)"
    head_path = f"{storage_path}/HEAD"
    if os.path.exists(head_path):
        with open(head_path, "r") as f:
            head = f.read().strip()

    print(f"HEAD: {head}")
    print("=" * 70)

    # Read all refs and commits
    refs_dir = f"{storage_path}/refs"
    if not os.path.exists(refs_dir):
        print("(no refs directory)")
        return

    # Build commit hash to ref mapping for easier reading
    hash_to_ref = {}
    commits_data = {}

    for ref_name in sorted(os.listdir(refs_dir)):
        ref_path = f"{refs_dir}/{ref_name}"
        with open(ref_path, "r") as f:
            commit_hash = f.read().strip()
        hash_to_ref[commit_hash] = ref_name

        try:
            data = read_object(storage_path, commit_hash)
            commit = parse_commit(data)
            if commit:
                commits_data[ref_name] = {
                    'hash': commit_hash,
                    'commit': commit
                }
        except Exception as e:
            commits_data[ref_name] = {'error': str(e)}

    merge_count = 0
    for ref_name in sorted(commits_data.keys()):
        info = commits_data[ref_name]
        if 'error' in info:
            print(f"\n{ref_name}: ERROR - {info['error']}")
            continue

        commit = info['commit']
        commit_hash = info['hash']
        ts = datetime.fromtimestamp(commit['timestamp'] / 1000).strftime('%Y-%m-%d %H:%M:%S')
        merge_badge = " [MERGE]" if commit['is_merge'] else ""
        if commit['is_merge']:
            merge_count += 1

        print(f"\n{ref_name}{merge_badge}")
        if verbose:
            print(f"  commit:  {commit_hash}")
        else:
            print(f"  commit:  {commit_hash[:20]}...")
        print(f"  time:    {ts}")
        print(f"  message: {commit['message']}")
        print(f"  parents: {len(commit['parent_hashes'])}")
        for i, ph in enumerate(commit['parent_hashes']):
            # Try to find which ref this parent belongs to
            parent_ref = hash_to_ref.get(ph, "")
            if parent_ref:
                parent_ref = f" ({parent_ref})"
            if verbose:
                print(f"    [{i+1}] {ph}{parent_ref}")
            else:
                print(f"    [{i+1}] {ph[:20]}...{parent_ref}")

    # Count objects
    objects_count = 0
    objects_dir = f"{storage_path}/objects"
    if os.path.exists(objects_dir):
        for prefix_dir in os.listdir(objects_dir):
            prefix_path = os.path.join(objects_dir, prefix_dir)
            if os.path.isdir(prefix_path):
                objects_count += len(os.listdir(prefix_path))

    print()
    print("=" * 70)
    print(f"Total objects: {objects_count}")
    print(f"Total refs: {len(os.listdir(refs_dir))}")
    print(f"Merge commits: {merge_count}")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 check_storage.py /path/to/project [-v]")
        print("  -v  verbose mode (full hashes)")
        sys.exit(1)

    project_path = sys.argv[1]
    verbose = "-v" in sys.argv
    check_storage(project_path, verbose)
