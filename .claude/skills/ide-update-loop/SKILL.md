---
name: ide-update-loop
description: Check for new IDE versions of every supported platform, build the plugin against them, analyze the failures and write a report to decide on. Changes nothing in the checkout; accepted items go to /ide-update-fix.
disable-model-invocation: true
---

# IDE update loop: check → build → analyze → report

One **run** of the loop ends in a report the user decides on. The run edits nothing in the checkout: the
new IDE versions reach Gradle as `-P` overrides, and all evidence lands in `build/ide-update-loop/<run>/`.
Applying what the user accepts is `/ide-update-fix`.

Scripts live in `.claude/skills/ide-update-loop/scripts/` (Python 3, standard library; `python` on Windows,
`python3` elsewhere). Each prints its options with `--help`. `$ARGUMENTS` may name environments (`262 263`)
or `full-tests`.

## 1. Check

Run `check_ide_updates.py` (with `--envs 262,263` when the user named environments). It starts the run
directory, records it in `build/ide-update-loop/LATEST`, and writes `updates.json`.

Done when every environment has a verdict: a proposed **bump** or up to date, and any newly published
platform branch is noted.

- No bumps and nothing else asked: show the table, say the plugin is up to date, and end the run.
- A new branch (say 271) goes into the report as a note. Supporting it is the procedure in
  `documentation/SupportNewPlatformVersion.md`, outside this loop.

## 2. Build

Note `git status` first: the build compiles uncommitted changes too, and the report must say so.

For each environment with a bump, one at a time, start `run_build.py --env <N>` in the background and wait
for it to exit (`--full-tests` when the user asked for it). One environment takes 15–40 minutes; the first
build against a new IDE also downloads it (several GB per product).

- Exit code 3: another Gradle build is running. Two builds in one checkout corrupt each other's output.
  Wait for it, or build from a git worktree and add
  `--gradle-arg=-Porg.jetbrains.intellij.platform.intellijPlatformIdesCache=<main checkout>/.intellijPlatform/ides`
  to reuse the downloaded IDEs.
- Exit code 1 is a red build: that is the input of step 3.

Done when every bumped environment has `summary.json` with each stage passed, failed or skipped.

## 3. Analyze

Run `analyze_build.py`. It writes `analysis.md`: the stage table and numbered error groups, each with a
class (`platform-change`, `known-cause`, `flake`, `environment`, `bump`, `new`) and, when known, a hint.

Then take every group to a **cause**:

- `environment`, `flake`: apply the hint once (clean the named cache, or rerun the failing classes with
  `run_build.py --env <N> --stages test --test-tasks "<task> --tests <class>" --label rerun`). A green rerun
  closes the group; a red one makes it `new`.
- `platform-change`: run `diff_platform.py <current IDE> <proposed IDE>` once per product (both IDEs sit in
  `.intellijPlatform/ides/` after the build). Read the code at the reported location and the API in the new
  IDE's jars. State what changed (removed, renamed, moved to module X, signature changed) and on which
  supported environments the replacement exists.
- `new`: decide whether the bump caused it. The build is **bump-caused** only when the same build is green
  on the current versions: the last CI run on `main` covers the CI environments (`ci_environments` in
  `updates.json`); for the others rebuild the failing stage with `--no-overrides --label baseline`.
- `bump`: the proposed version is unusable; propose the next one from the check table.

`reference/platform-breakage.md` holds the causes this repo has met and their fix patterns: read it before
writing a cause for any `platform-change`, `known-cause` or `new` group.

Done when every group has a cause with evidence (file:line, jar or diff line, baseline result) and a
proposed fix with its place (common code, `branches/<N>/src`, build script, properties file), or is marked
"needs investigation" with what was tried.

## 4. Report

Write `build/ide-update-loop/<run>/report.md`, in the language the user writes in:

1. One line per environment: current → proposed versions, green or red.
2. The stage table from `analysis.md`.
3. **Items**, numbered across the report. A green bump is one item ("apply 262 → 2026.2.5, no code
   change"). Each error group with a cause is one item: cause, evidence, proposed fix and where it goes,
   the environments it touches, risk.
4. **Not covered**: environments not built, stages not run, and runtime-only breakage the run cannot see
   (see "Runtime" in `reference/platform-breakage.md`).

In the chat, give the report path and the numbered one-line list of items, and ask which to accept. The
user answers with `/ide-update-fix <numbers>`.

Done when the report exists and every item is something the user can accept or reject on its own.
