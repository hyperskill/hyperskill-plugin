#!/usr/bin/env python3
"""Turn the evidence of run_build.py into error groups with a class and a signature.

Classes:
  platform-change  the bumped IDE changed or moved an API (compile errors, missing classes at runtime)
  known-cause      a failure this repo has met before, with a known fix (see the hint)
  flake            a known flaky failure: rerun once, a pass means no regression
  environment      caches, memory, network or another build in the checkout: not the code
  bump             the proposed version itself is unusable (cannot be resolved)
  new              nothing above matches: needs investigation

Usage:
  analyze_build.py [--run-dir DIR]          # every <env>[-label] directory of the run
  analyze_build.py --compare BEFORE AFTER   # two env directories: fixed / still failing / new

Writes <run>/analysis.md and <env dir>/analysis.json (the signatures --compare reads).
Standard library only.
"""

import argparse
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

from _common import repo_root, run_dir

# (id, class, pattern, hint) -- matched against failure messages, stack traces and failed-stage logs.
KNOWN = [
    ("windows-temp-lock", "flake", r"FileSystemException[\s\S]{0,300}being used by another process",
     "Windows holds a file of %TEMP%\\unitTest_* open in teardown; a different test fails every run. Rerun the class once."),
    ("sandbox-index", "environment",
     r"StubUpdatingIndex[\s\S]{0,3000}StorageException|StorageException[\s\S]{0,3000}StubUpdatingIndex|index \w+ has status REQUIRES_REBUILD",
     "Stale or corrupted test sandbox indexes (e.g. after the set of bundled/marketplace plugins changed). Rerun the "
     "class once; if it fails again, delete system-test and config-test under .intellijPlatform/sandbox/<module>/<IDE>/."),
    ("truncated-jar", "environment", r"ZipResourceFile|zip END header not found|error in opening zip file|ZipException: invalid",
     "Truncated jar in the Gradle dependency cache (a killed run): delete that artifact's version dir under <GRADLE_USER_HOME>/caches/modules-2."),
    ("build-cache", "environment", r"Corrupted TAR archive|invalid distance too far back|Failed to load cache entry",
     "Corrupted entry in the build cache: Gradle renames it to *.failed; delete those and rerun."),
    ("gradle-journal", "environment", r"CorruptedCacheException",
     "Corrupted <GRADLE_USER_HOME>/caches/journal-1/file-access.bin: move it aside when no Gradle runs."),
    ("checkout-collision", "environment", r"Unable to delete directory|Could not delete [^\n]*caches-jvm|Source file or directory not found",
     "Another build ran in the same checkout. Rerun alone or from a worktree; no test result of this run counts."),
    ("out-of-memory", "environment", r"OutOfMemoryError|Java heap space|GC overhead limit exceeded|daemon disappeared unexpectedly",
     "The build or test JVM ran out of memory; close other builds and rerun."),
    ("dead-space-repo", "known-cause", r"maven\.pkg\.jetbrains\.space",
     "JetBrains Space package hosting is shut down (the host no longer resolves). Gradle asks repositories in order, "
     "so this one fails every artifact not yet cached. Remove it from the repositories block; its artifacts are on "
     "Maven Central or packages.jetbrains.team."),
    ("network", "environment", r"UnknownHostException|Connection (timed out|reset|refused)|Read timed out|Could not (GET|HEAD) 'http",
     "Network failure while downloading. Rerun once; if the same host fails again, check that it resolves at all "
     "(`nslookup <host> 1.1.1.1`): a dead repository host fails every uncached artifact."),
    ("expired-build", "bump", r"This IDE build has expired",
     "The EAP/snapshot build expired; use a release or a newer snapshot."),
    ("unresolvable-ide", "bump", r"Could not (find|resolve)[^\n]*(com\.jetbrains\.intellij|jetbrains\.intellij\.(idea|clion|pycharm|rider))",
     "The proposed IDE version is not in the IntelliJ Maven repository; pick another one."),
    ("grazie-classpath", "known-cause", r"NoSuchMethodError[\s\S]{0,500}ai\.grazie|ai\.grazie[\s\S]{0,500}NoSuchMethodError|Language\.getEntries",
     "A bundled plugin on the test classpath clashes with the AI debugger's ai.grazie libraries: add its id to "
     "org.jetbrains.intellij.platform.testIdeBundledPluginsClasspathExcludes in gradle.properties."),
    ("bundled-plugin-init", "known-cause", r"lib[\\/]modules should be lib directory",
     "A bundled plugin fails to initialize on the test classpath: exclude it with the same gradle.properties key."),
    ("rust-test-service", "known-cause", r"TestCargoProjectsServiceImpl",
     "The Rust plugin needs its unshipped test service: keep it in idea.suppressed.plugins.id for :intellij-plugin:test."),
    ("missing-test-dep", "known-cause", r"NoClassDefFoundError: okhttp3/mockwebserver",
     "Test dependencies of hs-core come from the common conventions; check the module applies them."),
    ("class-moved", "platform-change", r"NoClassDefFoundError|ClassNotFoundException|Cannot access class",
     "A platform class left the classpath (moved into a content module or plugin): run diff_platform.py, declare "
     "bundledModule(...) for compilation and <module name> in the descriptor for runtime."),
]
KNOWN = [(kid, cls, re.compile(pattern), hint) for kid, cls, pattern, hint in KNOWN]

KOTLIN_ERROR = re.compile(r"^e: (?:file:///)?(.+?\.kts?):(\d+):(\d+) (.+)$")
JAVA_ERROR = re.compile(r"^(.+?\.java):(\d+): error: (.+)$")
FAILED_TASK = re.compile(r"^> Task (\S+) FAILED")


def match_known(text):
    for kid, cls, pattern, hint in KNOWN:
        if pattern.search(text):
            return kid, cls, hint
    return None


def module_of(path, root):
    try:
        rel = Path(path).resolve().relative_to(root).as_posix()
    except ValueError:
        rel = Path(path).as_posix()
    for marker in ("/src/", "/testSrc/", "/branches/", "/resources/"):
        if marker in rel:
            return rel.split(marker, 1)[0], rel
    return rel.rsplit("/", 1)[0], rel


def what_went_wrong(text):
    blocks = re.findall(r"\* What went wrong:\n([\s\S]*?)(?:\n\* Try:|\Z)", text)
    return [b.strip()[:1500] for b in blocks]


def analyze_log_stage(name, log_text, root):
    """Compile-style analysis: compiler errors grouped by message, else Gradle's own failure text."""
    groups = {}
    lines = log_text.splitlines()
    for i, line in enumerate(lines):
        match = KOTLIN_ERROR.match(line)
        java = JAVA_ERROR.match(line) if not match else None
        if match:
            path, line_no, message = match.group(1), match.group(2), match.group(4).strip()
        elif java:
            path, line_no, message = java.group(1), java.group(2), java.group(3).strip()
            symbol = next((l.strip() for l in lines[i + 1:i + 4] if l.strip().startswith("symbol:")), "")
            if symbol:
                message = f"{message} ({symbol})"
        else:
            continue
        module, rel = module_of(path, root)
        group = groups.setdefault(message, {"message": message, "locations": [], "modules": set()})
        group["locations"].append(f"{rel}:{line_no}")
        group["modules"].add(module)

    result = []
    for group in groups.values():
        known = match_known(group["message"])
        result.append({
            "stage": name,
            "class": known[1] if known else "platform-change",
            "known_id": known[0] if known else None,
            "hint": known[2] if known else None,
            "title": group["message"],
            "count": len(group["locations"]),
            "modules": sorted(group["modules"]),
            "locations": group["locations"][:10],
            "signatures": sorted({f"{name}|{loc.rsplit(':', 1)[0]}|{group['message']}" for loc in group["locations"]}),
        })
    if not result:
        failed_tasks = [m.group(1) for m in map(FAILED_TASK.match, lines) if m]
        for block in what_went_wrong(log_text) or ["\n".join(lines[-30:])]:
            known = match_known(block) or match_known(log_text)
            first = block.splitlines()[0] if block else "no output"
            result.append({
                "stage": name,
                "class": known[1] if known else "new",
                "known_id": known[0] if known else None,
                "hint": known[2] if known else None,
                "title": first[:200],
                "detail": block,
                "count": 1,
                "modules": failed_tasks,
                "signatures": [f"{name}|gradle|{first[:200]}"],
            })
    return result


def normalize(message):
    message = re.sub(r"[A-Za-z]:[\\/][^\s'\"]+|/(tmp|var|private)/[^\s'\"]+", "<path>", message)
    return re.sub(r"\d+", "N", message)[:200]


def analyze_tests(env_dir):
    groups = {}
    for xml_path in sorted((env_dir / "test-results").glob("**/TEST-*.xml")):
        module = xml_path.parent.name.replace("__", "/")
        try:
            suite = ET.parse(xml_path).getroot()
        except ET.ParseError:
            continue
        for case in suite.iter("testcase"):
            problem = case.find("failure")
            if problem is None:
                problem = case.find("error")
            if problem is None:
                continue
            message = (problem.get("message") or "").strip()
            body = problem.text or ""
            kind = problem.get("type") or "failure"
            first = message.splitlines()[0] if message else (body.strip().splitlines() or [""])[0]
            key = (kind, normalize(first))
            group = groups.setdefault(key, {"type": kind, "first": first, "tests": [], "modules": set(), "text": ""})
            group["tests"].append(f"{case.get('classname')}.{case.get('name')}")
            group["modules"].add(module)
            if len(group["text"]) < 8000:
                group["text"] += f"{message}\n{body[:4000]}\n"

    result = []
    for group in groups.values():
        known = match_known(group["text"])
        result.append({
            "stage": "test",
            "class": known[1] if known else "new",
            "known_id": known[0] if known else None,
            "hint": known[2] if known else None,
            "title": group["first"][:200] if group["first"].startswith(group["type"]) else f"{group['type']}: {group['first'][:200]}",
            "count": len(group["tests"]),
            "modules": sorted(group["modules"]),
            "tests": group["tests"][:10],
            "excerpt": group["text"][:1200],
            "signatures": sorted(f"test|{t}" for t in group["tests"]),
        })
    return result


def analyze_verifier(env_dir, log_text):
    result = []
    for verdict in sorted((env_dir / "verifier").glob("*/verification-verdict.txt")):
        text = verdict.read_text(encoding="utf-8", errors="replace").strip()
        ide = verdict.parent.name
        problems_file = verdict.parent / "compatibility-problems.txt"
        if text.startswith("Compatible") and not problems_file.exists():
            continue
        problems = problems_file.read_text(encoding="utf-8", errors="replace") if problems_file.exists() else ""
        lines = [l for l in problems.splitlines() if l.strip()]
        result.append({
            "stage": "verify",
            "class": "platform-change",
            "known_id": None,
            "hint": "Plugin Verifier sees what tests can't: content-module classloaders and inlined code. "
                    "See reference/platform-breakage.md.",
            "title": f"{ide}: {text.splitlines()[0][:200]}",
            "count": max(1, len(lines)),
            "modules": [],
            "detail": "\n".join(lines[:60]),
            "signatures": [f"verify|{ide.split('-')[0]}|{l.strip()[:200]}" for l in lines[:200]] or [f"verify|{text[:200]}"],
        })
    return result


def analyze_env(env_dir, root):
    summary = json.loads((env_dir / "summary.json").read_text(encoding="utf-8"))
    groups = []
    for stage, info in summary["stages"].items():
        if info["status"] != "failed":
            continue
        log_text = (env_dir / info["log"]).read_text(encoding="utf-8", errors="replace")
        if stage == "test":
            stage_groups = analyze_tests(env_dir)
            if not stage_groups:  # the task failed before any test ran
                stage_groups = analyze_log_stage(stage, log_text, root)
        elif stage == "verify":
            stage_groups = analyze_verifier(env_dir, log_text) or analyze_log_stage(stage, log_text, root)
        else:
            stage_groups = analyze_log_stage(stage, log_text, root)
        groups.extend(stage_groups)
    signatures = sorted({s for g in groups for s in g["signatures"]})
    signatures += [f"stage|{name}|{info['status']}" for name, info in summary["stages"].items()]
    analysis = {"env_dir": env_dir.name, "summary": summary, "groups": groups, "signatures": signatures}
    (env_dir / "analysis.json").write_text(json.dumps(analysis, indent=2), encoding="utf-8")
    return analysis


def render(run, analyses):
    order = {"bump": 0, "platform-change": 1, "new": 2, "known-cause": 3, "environment": 4, "flake": 5}
    out = [f"# IDE update loop: analysis of {run.name}", ""]
    updates = run / "updates.json"
    if updates.exists():
        data = json.loads(updates.read_text(encoding="utf-8"))
        out += ["## Proposed versions", "", "| env | property | current | proposed |", "|---|---|---|---|"]
        for env, info in data["environments"].items():
            for prop, value in info["overrides"].items():
                out.append(f"| {env} | {prop} | {info['products'][prop]['current']} | {value} |")
        out.append("")
    out += ["## Stages", "", "| build | overrides | compile | test | verify |", "|---|---|---|---|---|"]
    for a in analyses:
        stages = a["summary"]["stages"]

        def cell(name):
            info = stages.get(name)
            if not info:
                return "-"
            text = f"{info['status']}"
            if "duration_s" in info:
                text += f" ({info['duration_s'] // 60}m)"
            totals = info.get("totals")
            if totals:
                text += f", {totals['failures'] + totals['errors']}/{totals['tests']} failed"
            return text

        overrides = ", ".join(a["summary"]["overrides"].values()) or "properties file"
        out.append(f"| {a['env_dir']} | {overrides} | {cell('compile')} | {cell('test')} | {cell('verify')} |")
    out.append("")
    number = 0
    for a in analyses:
        if not a["groups"]:
            continue
        out += [f"## {a['env_dir']}: error groups", ""]
        for g in sorted(a["groups"], key=lambda g: (order.get(g["class"], 9), -g["count"])):
            number += 1
            out.append(f"### {number}. [{g['class']}] {g['stage']}: {g['title']}")
            out.append(f"- count: {g['count']}; modules: {', '.join(g['modules']) or '-'}")
            if g.get("known_id"):
                out.append(f"- known: `{g['known_id']}`: {g['hint']}")
            elif g.get("hint"):
                out.append(f"- hint: {g['hint']}")
            for key in ("locations", "tests"):
                if g.get(key):
                    out.append(f"- {key}: " + ", ".join(f"`{x}`" for x in g[key][:6]) +
                               (f" (+{g['count'] - 6} more)" if g["count"] > 6 else ""))
            for key in ("detail", "excerpt"):
                if g.get(key):
                    out += ["", "```", g[key].strip()[:1500], "```"]
            out.append("")
    if number == 0:
        out.append("No failures.")
    return "\n".join(out) + "\n"


def compare(before_dir, after_dir):
    """Error signatures of the stages that ran in both builds: an after-build of only `compile`
    says nothing about the tests of the before-build."""
    before_a = json.loads((before_dir / "analysis.json").read_text(encoding="utf-8"))
    after_a = json.loads((after_dir / "analysis.json").read_text(encoding="utf-8"))
    ran = lambda a: {n for n, i in a["summary"]["stages"].items() if i["status"] != "skipped"}  # noqa: E731
    common = ran(before_a) & ran(after_a)
    errors = lambda a: {x for x in a["signatures"] if x.split("|", 1)[0] in common}  # noqa: E731
    before, after = set(before_a["signatures"]), set(after_a["signatures"])
    fixed = errors(before_a) - errors(after_a)
    persisting = errors(before_a) & errors(after_a)
    new = errors(after_a) - errors(before_a)
    print(f"Compare {before_dir.name} -> {after_dir.name} on stages: {', '.join(sorted(common)) or 'none'}")
    only_after = ran(after_a) - common
    if only_after:
        print(f"Ran only in {after_dir.name} (read its analysis.md groups): {', '.join(sorted(only_after))}")
    print("stages before: " + ", ".join(sorted(x[6:] for x in before if x.startswith("stage|"))))
    print("stages after:  " + ", ".join(sorted(x[6:] for x in after if x.startswith("stage|"))))
    for title, items in (("fixed", fixed), ("still failing", persisting), ("NEW", new)):
        print(f"\n{title}: {len(items)}")
        for item in sorted(items)[:40]:
            print(f"  {item}")
    return 1 if new else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--run-dir")
    parser.add_argument("--repo-root")
    parser.add_argument("--compare", nargs=2, metavar=("BEFORE", "AFTER"))
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    root = repo_root(args.repo_root)
    run = run_dir(root, args.run_dir)

    if args.compare:
        dirs = [Path(d) if Path(d).is_absolute() else run / d for d in args.compare]
        for d in dirs:
            if not (d / "analysis.json").exists():
                analyze_env(d, root)
        sys.exit(compare(*dirs))

    env_dirs = sorted(d for d in run.iterdir() if (d / "summary.json").exists())
    if not env_dirs:
        sys.exit(f"No builds in {run}: run run_build.py first")
    analyses = [analyze_env(d, root) for d in env_dirs]
    report = render(run, analyses)
    (run / "analysis.md").write_text(report, encoding="utf-8")
    print(report)
    print(f"Written: {run / 'analysis.md'}")


if __name__ == "__main__":
    main()
