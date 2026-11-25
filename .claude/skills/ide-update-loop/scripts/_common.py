"""Paths shared by the ide-update-loop scripts. Standard library only."""

import subprocess
from datetime import datetime
from pathlib import Path


def repo_root(arg=None):
    """The checkout the scripts work on: --repo-root, else the git top level of the current directory."""
    if arg:
        return Path(arg).resolve()
    try:
        out = subprocess.run(["git", "rev-parse", "--show-toplevel"], capture_output=True, text=True, check=True)
        return Path(out.stdout.strip()).resolve()
    except (OSError, subprocess.CalledProcessError):
        return Path(__file__).resolve().parents[4]


def loop_root(root):
    return root / "build" / "ide-update-loop"


def new_run_dir(root):
    run = loop_root(root) / datetime.now().strftime("%Y%m%d-%H%M%S")
    run.mkdir(parents=True, exist_ok=False)
    (loop_root(root) / "LATEST").write_text(run.name, encoding="utf-8")
    return run


def run_dir(root, arg=None):
    """--run-dir if given, else the run named in build/ide-update-loop/LATEST."""
    if arg:
        return Path(arg).resolve()
    latest = loop_root(root) / "LATEST"
    if not latest.exists():
        raise SystemExit("No run yet: start one with check_ide_updates.py")
    return loop_root(root) / latest.read_text(encoding="utf-8").strip()


def read_properties(path):
    props = {}
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        props[key.strip()] = value.strip()
    return props
