#!/usr/bin/env python3
"""Build one environment the way CI does, against the proposed IDE versions, and keep the evidence.

Stages (CI order): compile -> test -> verify. A red compile skips the rest; a red test still runs verify.
IDE versions are passed as -P overrides (they beat gradle-<env>.properties), so nothing in the
checkout is edited. Every stage's full log, the failing JUnit XML files and the Plugin Verifier
verdicts are saved to <run>/<env>[-<label>]/, with summary.json describing the run.

Usage:
  run_build.py --env 262                       # proposed versions from <run>/updates.json
  run_build.py --env 262 --no-overrides        # the versions in gradle-262.properties (baseline)
  run_build.py --env 262 --stages compile --label fix1
  run_build.py --env 262 --stages test --test-tasks ":intellij-plugin:test --tests org.hyperskill.academy.FooTest"

Options: --override prop=value (repeatable), --full-tests (`test --continue` on every module),
--gradle-arg ARG (repeatable, appended to every Gradle call), --run-dir DIR, --allow-concurrent.
Standard library only.
"""

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path

from _common import repo_root, run_dir

STAGES = {
    "compile": [":intellij-plugin:testClasses", ":hs-edu-format:testClasses", ":hs-framework-storage:testClasses"],
    "test": [":intellij-plugin:test", ":hs-edu-format:test", ":hs-framework-storage:test", "--continue"],
    "verify": ["verifyPlugin"],
}
ECHO = re.compile(r"FAILED|BUILD (SUCCESSFUL|FAILED)|tests completed|^e: |: error: |What went wrong")


def gradle_clients():
    """Command-line Gradle builds running on this machine (pid and arguments), or None if it can't tell."""
    if os.name == "nt":
        script = ("Get-CimInstance Win32_Process -Filter \"Name='java.exe'\" | "
                  "Where-Object { $_.CommandLine -match 'gradle-wrapper.jar|GradleWrapperMain' } | "
                  "ForEach-Object { \"$($_.ProcessId) $($_.CommandLine)\" }")
        cmd = ["powershell", "-NoProfile", "-NonInteractive", "-Command", script]
    else:
        cmd = ["ps", "-eo", "pid,args"]
    try:
        out = subprocess.run(cmd, capture_output=True, text=True, timeout=60).stdout
    except (OSError, subprocess.SubprocessError):
        return None
    clients = [line.strip() for line in out.splitlines()
               if "GradleWrapperMain" in line or "gradle-wrapper.jar" in line]
    # Each client shows a long classpath; keep the pid and what it builds.
    return [re.sub(r"\s-(classpath|cp)\s+\S+", "", c)[-300:] for c in clients]


def run_stage(root, cmd, log_path):
    echoed = 0
    with open(log_path, "w", encoding="utf-8") as log:
        log.write("$ " + " ".join(cmd) + "\n")
        process = subprocess.Popen(cmd, cwd=root, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                                   text=True, encoding="utf-8", errors="replace")
        for line in process.stdout:
            log.write(line)
            if ECHO.search(line) and echoed < 40:
                print("   " + line.rstrip())
                echoed += 1
        return process.wait()


def junit_files(root):
    """TEST-*.xml under every module's build/test-results, without walking IDE sandboxes or caches."""
    for current, dirs, _ in os.walk(root):
        dirs[:] = [d for d in dirs if not d.startswith(".") and d not in ("node_modules", "testData")]
        if Path(current).name == "build":
            yield from (Path(current) / "test-results").glob("**/TEST-*.xml")
            dirs[:] = []


def collect_tests(root, since, dest):
    """Totals over JUnit XML written after `since`; copies the files that hold failures."""
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0, "files": 0}
    for xml_path in junit_files(root):
        if xml_path.stat().st_mtime < since:
            continue
        try:
            suite = ET.parse(xml_path).getroot()
        except ET.ParseError:
            continue
        totals["files"] += 1
        for key in ("tests", "failures", "errors", "skipped"):
            totals[key] += int(suite.get(key, 0) or 0)
        if int(suite.get("failures", 0) or 0) + int(suite.get("errors", 0) or 0):
            module = xml_path.relative_to(root).parts[: xml_path.relative_to(root).parts.index("build")]
            target = dest / "__".join(module) / xml_path.name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(xml_path, target)
    return totals


def collect_verifier(root, since, dest):
    reports = root / "intellij-plugin" / "build" / "reports" / "pluginVerifier"
    copied = []
    for txt in reports.glob("*/plugins/*/*/*.txt"):
        if txt.stat().st_mtime < since:
            continue
        ide = txt.relative_to(reports).parts[0]
        target = dest / ide / txt.name
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(txt, target)
        copied.append(f"{ide}/{txt.name}")
    return copied


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--env", required=True)
    parser.add_argument("--stages", default="compile,test,verify")
    parser.add_argument("--override", action="append", default=[], metavar="PROP=VALUE")
    parser.add_argument("--no-overrides", action="store_true", help="ignore updates.json")
    parser.add_argument("--label", help="suffix of the output directory, e.g. baseline or fix1")
    parser.add_argument("--full-tests", action="store_true")
    parser.add_argument("--test-tasks", help="replaces the test stage's task list")
    parser.add_argument("--gradle-arg", action="append", default=[], metavar="ARG")
    parser.add_argument("--run-dir")
    parser.add_argument("--repo-root")
    parser.add_argument("--allow-concurrent", action="store_true")
    args = parser.parse_args()
    # Line-buffered: the script usually runs in the background and its output file is read while it works.
    sys.stdout.reconfigure(encoding="utf-8", errors="replace", line_buffering=True)

    root = repo_root(args.repo_root)
    run = run_dir(root, args.run_dir)
    stages = [s.strip() for s in args.stages.split(",") if s.strip()]
    unknown = [s for s in stages if s not in STAGES]
    if unknown:
        sys.exit(f"Unknown stage(s): {', '.join(unknown)}")

    overrides = {}
    updates_file = run / "updates.json"
    if not args.no_overrides and updates_file.exists():
        env_updates = json.loads(updates_file.read_text(encoding="utf-8"))["environments"].get(args.env, {})
        overrides.update(env_updates.get("overrides", {}))
    for item in args.override:
        key, _, value = item.partition("=")
        overrides[key] = value

    clients = gradle_clients()
    if clients and not args.allow_concurrent:
        print("Other Gradle builds are running:\n  " + "\n  ".join(clients))
        print("Two builds in one checkout break each other. Wait for them, build from a worktree, "
              "or pass --allow-concurrent if they run elsewhere.")
        sys.exit(3)
    if clients is None:
        print("Could not list processes: check for other Gradle builds yourself.")

    out = run / (f"{args.env}-{args.label}" if args.label else args.env)
    out.mkdir(parents=True, exist_ok=True)
    gradlew = str(root / ("gradlew.bat" if os.name == "nt" else "gradlew"))
    head = subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=root, capture_output=True, text=True).stdout.strip()
    summary = {
        "env": args.env, "label": args.label, "overrides": overrides, "git_head": head,
        "started_at": datetime.now().isoformat(timespec="seconds"), "stages": {},
    }
    print(f"Building {args.env} in {root} (HEAD {head}) with {overrides or 'the versions from the properties file'}")

    compile_failed = False
    for stage in stages:
        if compile_failed:
            summary["stages"][stage] = {"status": "skipped", "reason": "compile failed"}
            continue
        tasks = STAGES[stage]
        if stage == "test" and args.full_tests:
            tasks = ["test", "--continue"]
        if stage == "test" and args.test_tasks:
            tasks = args.test_tasks.split()
        cmd = [gradlew, *tasks, f"-PenvironmentName={args.env}",
               *[f"-P{k}={v}" for k, v in overrides.items()], *args.gradle_arg, "--console=plain"]
        print(f"== {stage}: {' '.join(tasks)}")
        started = time.time()
        code = run_stage(root, cmd, out / f"{stage}.log")
        result = {"status": "passed" if code == 0 else "failed", "exit_code": code,
                  "duration_s": round(time.time() - started), "log": f"{stage}.log", "command": cmd[1:]}
        if stage == "test":
            result["totals"] = collect_tests(root, started, out / "test-results")
        if stage == "verify":
            result["reports"] = collect_verifier(root, started, out / "verifier")
        summary["stages"][stage] = result
        print(f"== {stage}: {result['status']} in {result['duration_s']}s")
        compile_failed = stage == "compile" and code != 0
        (out / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")

    summary["finished_at"] = datetime.now().isoformat(timespec="seconds")
    (out / "summary.json").write_text(json.dumps(summary, indent=2), encoding="utf-8")
    print(f"Evidence: {out}")
    sys.exit(0 if all(s["status"] != "failed" for s in summary["stages"].values()) else 1)


if __name__ == "__main__":
    main()
