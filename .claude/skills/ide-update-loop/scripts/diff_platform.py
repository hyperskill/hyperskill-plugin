#!/usr/bin/env python3
"""Diff the layout of two extracted IDEs: what left the core classpath, which modules and plugins
appeared, disappeared or changed kind.

On 2026.3 most compile and runtime breakage came from jars leaving the `com.intellij` classPath
into content modules with their own classloader; this diff predicts that before reading any error.

Usage:
  diff_platform.py IU-2026.2.1 IU-2026.2.5     # directories under <repo>/.intellijPlatform/ides
  diff_platform.py <path-to-old-ide> <path-to-new-ide>

The new IDE exists there once a build has resolved it (run_build.py with the proposed version).
Standard library only.
"""

import argparse
import json
import sys
from pathlib import Path

from _common import repo_root


def load(arg, root):
    path = Path(arg)
    if not path.is_dir():
        path = root / ".intellijPlatform" / "ides" / arg
    info = path / "product-info.json"
    if not info.exists():
        sys.exit(f"{info} not found: the IDE has not been resolved yet (build against it first)")
    return json.loads(info.read_text(encoding="utf-8")), path.name


def section(title, items, limit=80):
    items = sorted(items)
    if not items:
        return []
    lines = [f"### {title} ({len(items)})", ""]
    lines += [f"- `{item}`" for item in items[:limit]]
    if len(items) > limit:
        lines.append(f"- ... {len(items) - limit} more")
    return lines + [""]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("old")
    parser.add_argument("new")
    parser.add_argument("--repo-root")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    root = repo_root(args.repo_root)
    old, old_name = load(args.old, root)
    new, new_name = load(args.new, root)

    def core(info):
        entry = next((e for e in info.get("layout", []) if e.get("name") == "com.intellij"), {})
        return {Path(p).name for p in entry.get("classPath", [])}

    def layout(info):
        return {e["name"]: e.get("kind") for e in info.get("layout", [])}

    def bundled(info):
        return set(info.get("bundledPlugins", []))

    old_layout, new_layout = layout(old), layout(new)
    kind_changes = {f"{name}: {old_layout[name]} -> {new_layout[name]}"
                    for name in old_layout.keys() & new_layout.keys() if old_layout[name] != new_layout[name]}

    out = [f"## Platform diff {old_name} ({old.get('buildNumber')}) -> {new_name} ({new.get('buildNumber')})", ""]
    out += section("Jars that left the core classPath: code using them needs bundledModule(...) / <module name>",
                   core(old) - core(new))
    out += section("Jars that joined the core classPath", core(new) - core(old))
    out += section("Layout entries that changed kind", kind_changes)
    out += section("Layout entries removed (modules or plugins gone or renamed)",
                   {f"{n} ({k})" for n, k in old_layout.items() if n not in new_layout})
    out += section("Layout entries added", {f"{n} ({k})" for n, k in new_layout.items() if n not in old_layout})
    out += section("Bundled plugins removed", bundled(old) - bundled(new))
    out += section("Bundled plugins added", bundled(new) - bundled(old))
    if len(out) == 2:
        out.append("No layout differences: compile errors, if any, come from changed signatures, not moved classes.")
    print("\n".join(out))


if __name__ == "__main__":
    main()
