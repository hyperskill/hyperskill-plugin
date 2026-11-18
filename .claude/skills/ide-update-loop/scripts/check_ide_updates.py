#!/usr/bin/env python3
"""Compare the IDE versions in gradle-<env>.properties with the JetBrains Maven repository.

The build resolves IDEs from Maven (`useInstaller = false`), so the repository's
maven-metadata.xml is the list of versions the build can actually use.

For every environment (`gradle-252.properties`, `gradle-263.properties`, ...) and product
(ideaVersion, clionVersion, pycharmVersion, riderVersion) it reports the current version,
the newest release of the same platform branch and the newest pre-release, and proposes
a bump to that release. It also reports platform branches newer than every supported one.

Usage:
  check_ide_updates.py [--repo-root DIR] [--envs 261,262] [--json OUT | --no-save]

Prints a Markdown table. Unless --no-save, it starts a new run directory
build/ide-update-loop/<timestamp>/ (named in build/ide-update-loop/LATEST) and writes
updates.json there; run_build.py and analyze_build.py pick that run up by default.
Standard library only.
"""

import argparse
import json
import re
import sys
import urllib.request
import xml.etree.ElementTree as ET
from datetime import datetime, timezone
from pathlib import Path

from _common import new_run_dir, read_properties, repo_root

REPO_URL = "https://www.jetbrains.com/intellij-repository/{repo}/com/jetbrains/intellij/{artifact}/maven-metadata.xml"

# property name -> (product code, Maven artifact path for a platform branch)
PRODUCTS = {
    # 2025.3+ resolves the unified `idea` artifact (see intellijIde() in buildSrc/intellijUtils.kt)
    "ideaVersion": ("IU", lambda branch: "idea/idea" if branch >= 253 else "idea/ideaIU"),
    "clionVersion": ("CL", lambda branch: "clion/clion"),
    "pycharmVersion": ("PC", lambda branch: "pycharm/pycharmPC"),
    "riderVersion": ("RD", lambda branch: "rider/riderRD"),
}

RELEASE_RE = re.compile(r"^20\d\d\.\d+(\.\d+)*$")
STAGE_RANK = {"SNAPSHOT": 0, "EAP": 1, "RC": 2}


def branch_of(version):
    """Platform branch of a Maven version: 2026.2.5 -> 262, 263.8311.4-EAP-CANDIDATE -> 263, 2026.3-RC1-SNAPSHOT -> 263."""
    head = version.split("-", 1)[0]
    if head.startswith("20"):
        parts = head.split(".")
        if len(parts) < 2 or not parts[1].isdigit():
            return None
        return int(parts[0][2:] + parts[1])
    number = head.split(".", 1)[0]
    return int(number) if number.isdigit() else None


def is_release(version):
    return bool(RELEASE_RE.match(version))


def numeric(version):
    head = version.split("-", 1)[0]
    return tuple(int(p) for p in head.split(".") if p.isdigit())


def prerelease_key(version, index):
    """Order pre-releases: numeric part, then SNAPSHOT < EAPn < RCn, then publication order."""
    rest = version.split("-", 1)[1] if "-" in version else ""
    stage, stage_number = 0, 0
    match = re.search(r"\b(EAP|RC)(\d*)", rest)
    if match:
        stage = STAGE_RANK[match.group(1)]
        stage_number = int(match.group(2) or 0)
    return numeric(version), stage, stage_number, index


_metadata_cache = {}


def fetch_versions(repo, artifact):
    key = (repo, artifact)
    if key not in _metadata_cache:
        url = REPO_URL.format(repo=repo, artifact=artifact)
        request = urllib.request.Request(url, headers={"User-Agent": "ide-update-check"})
        with urllib.request.urlopen(request, timeout=60) as response:
            root = ET.fromstring(response.read())
        _metadata_cache[key] = [v.text.strip() for v in root.iter("version") if v.text]
    return _metadata_cache[key]


def check_product(env_branch, prop, value):
    code, artifact_of = PRODUCTS[prop]
    current_code, _, current = value.partition("-")
    artifact = artifact_of(env_branch)
    releases = [v for v in fetch_versions("releases", artifact) if is_release(v) and branch_of(v) == env_branch]
    snapshots = fetch_versions("snapshots", artifact)
    prereleases = [(v, i) for i, v in enumerate(snapshots) if not is_release(v) and branch_of(v) == env_branch]

    latest_release = max(releases, key=numeric) if releases else None
    latest_pre = max(prereleases, key=lambda p: prerelease_key(*p))[0] if prereleases else None

    proposal = None
    if latest_release:
        if not is_release(current) or numeric(latest_release) > numeric(current):
            proposal = f"{current_code}-{latest_release}"

    if proposal:
        status = "update"
    elif is_release(current):
        status = "up-to-date"
    else:
        status = "pre-release"  # no release of this branch yet: the build follows a snapshot or EAP

    return {
        "artifact": artifact,
        "current": value,
        "latest_release": f"{current_code}-{latest_release}" if latest_release else None,
        "latest_prerelease": f"{current_code}-{latest_pre}" if latest_pre else None,
        "proposal": proposal,
        "status": status,
    }


def newer_branches(max_branch):
    """Branches above every supported environment, with the versions that show them."""
    found = {}
    for prop, (code, artifact_of) in PRODUCTS.items():
        artifact = artifact_of(max_branch + 1)
        for repo in ("releases", "snapshots"):
            try:
                versions = fetch_versions(repo, artifact)
            except Exception:  # noqa: BLE001 - reported by the per-product check
                continue
            for v in versions:
                b = branch_of(v)
                if b and b > max_branch:
                    found.setdefault(b, set()).add(f"{code}-{v}")
    return [{"branch": b, "evidence": sorted(found[b])[-4:]} for b in sorted(found)]


def ci_environments(root):
    """Environments the CI matrix builds; the others are only ever checked locally."""
    workflow = root / ".github" / "workflows" / "ci.yaml"
    if not workflow.exists():
        return []
    match = re.search(r"environmentName:\s*\n((?:\s*-\s*\d{3}\s*\n)+)", workflow.read_text(encoding="utf-8"))
    return re.findall(r"\d{3}", match.group(1)) if match else []


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--repo-root")
    parser.add_argument("--envs", help="comma-separated environment names; default: every gradle-<env>.properties")
    output = parser.add_mutually_exclusive_group()
    output.add_argument("--json", help="write the result to this JSON file instead of a new run directory")
    output.add_argument("--no-save", action="store_true", help="only print the table")
    args = parser.parse_args()
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

    root = repo_root(args.repo_root)
    env_files = sorted(root.glob("gradle-[0-9][0-9][0-9].properties"))
    if not env_files:
        sys.exit(f"No gradle-<env>.properties under {root}")
    wanted = set(args.envs.split(",")) if args.envs else None

    result = {
        "checked_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
        "repo_root": str(root),
        "ci_environments": ci_environments(root),
        "environments": {},
        "new_branches": [],
        "errors": [],
    }

    for path in env_files:
        env = path.stem.split("-", 1)[1]
        if wanted and env not in wanted:
            continue
        props = read_properties(path)
        env_result = {"properties_file": path.name, "products": {}, "overrides": {}}
        for prop in PRODUCTS:
            if prop not in props:
                continue
            try:
                info = check_product(int(env), prop, props[prop])
            except Exception as error:  # noqa: BLE001 - one product's failure must not hide the others
                result["errors"].append(f"{env} {prop}: {error}")
                info = {"current": props[prop], "status": "error", "error": str(error)}
            env_result["products"][prop] = info
            if info.get("proposal"):
                env_result["overrides"][prop] = info["proposal"]
        env_result["has_updates"] = bool(env_result["overrides"])
        result["environments"][env] = env_result

    all_envs = [int(p.stem.split("-", 1)[1]) for p in env_files]
    result["new_branches"] = newer_branches(max(all_envs))

    print(f"IDE versions checked against the JetBrains Maven repository ({result['checked_at']})\n")
    print("| env | property | current | latest release | latest pre-release | status |")
    print("|---|---|---|---|---|---|")
    for env, env_result in result["environments"].items():
        for prop, info in env_result["products"].items():
            status = info["status"]
            if info.get("proposal"):
                status = f"**update -> {info['proposal']}**"
            print(f"| {env} | {prop} | {info['current']} | {info.get('latest_release') or '-'} "
                  f"| {info.get('latest_prerelease') or '-'} | {status} |")
    print()
    to_build = [env for env, r in result["environments"].items() if r["has_updates"]]
    print(f"Environments with updates: {', '.join(to_build) if to_build else 'none'}")
    local_only = [env for env in result["environments"] if env not in result["ci_environments"]]
    if result["ci_environments"]:
        print(f"CI builds {', '.join(result['ci_environments'])}; only checked locally: {', '.join(local_only) or 'none'}")
    for nb in result["new_branches"]:
        print(f"New platform branch {nb['branch']} is published: {', '.join(nb['evidence'])} "
              f"(not supported yet, see documentation/SupportNewPlatformVersion.md)")
    for error in result["errors"]:
        print(f"ERROR {error}")

    if not args.no_save:
        out = Path(args.json) if args.json else new_run_dir(root) / "updates.json"
        out.parent.mkdir(parents=True, exist_ok=True)
        out.write_text(json.dumps(result, indent=2), encoding="utf-8")
        print(f"\nRun directory: {out.parent}")

    total = sum(len(r["products"]) for r in result["environments"].values())
    if total and len(result["errors"]) == total:
        sys.exit(2)


if __name__ == "__main__":
    main()
