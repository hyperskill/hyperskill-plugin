---
name: ide-update-fix
description: Apply the IDE version bumps and fixes accepted from an /ide-update-loop report, proving each one with a rebuild until the affected platforms are green.
disable-model-invocation: true
---

# IDE update fix: apply accepted items under control

Input: item numbers from the latest `/ide-update-loop` report (`$ARGUMENTS`, e.g. `1 3 4`). The run is the
one named in `build/ide-update-loop/LATEST` unless the user names another. Scripts are those of the loop:
`.claude/skills/ide-update-loop/scripts/`.

**Control** means two things here: a change counts only when a rebuild proves it, and nothing reaches git
history or the remote without the user's word.

## 1. Prepare

- Read the run's `report.md` and `analysis.md`. Restate each accepted item in one line. Done when every
  number maps to exactly one item; ask about any that does not.
- Work on a branch: from an up-to-date `main`, create `ide-update/<YYYY-MM-DD>` (or continue the branch the
  user names). `main` itself stays untouched.
- One Gradle build per checkout at a time, as in the loop (`run_build.py` exits with 3 otherwise).

## 2. Apply

1. **Bumps**: write the proposed versions of the accepted environments into `gradle-<N>.properties`.
   `customSinceBuild` stays unless the item says otherwise.
2. **Fixes**: the smallest change that removes the cause stated in the item, following "Handling Platform
   Compatibility" in `CLAUDE.md` and `.claude/skills/ide-update-loop/reference/platform-breakage.md`.
   Common code must compile against every supported environment; a part that cannot goes to
   `branches/<N>/src` and gets a `// BACKCOMPAT: <N>` comment.

## 3. Verify

Iteration `k` = 1, 2, 3 for each accepted environment `<N>`:

1. `run_build.py --env <N> --no-overrides --label fix<k>`: the properties now hold the bump, so this builds
   exactly what will be committed. While compile is red, `--stages compile` is enough.
2. `analyze_build.py`, then `analyze_build.py --compare <N> <N>-fix<k>`.
3. **Green**: the item's signatures are under "fixed" and nothing is under "NEW"; stages that ran only in
   the new build are read from its groups in `analysis.md`. **Red**: read the new evidence, adjust, next `k`.

After three iterations without progress on an item, revert that item's change and record it as not fixed,
with what was tried.

Then check the **blast radius**: common code and build scripts are shared, so compile every other
supported environment with `run_build.py --env <M> --no-overrides --stages compile --label cross`.
Environments outside CI (`ci_environments` in `updates.json`) get no other check before release. When the
change touched shared runtime code, run all stages there, and follow "Runtime" in the reference when the
item names a moved module.

Done when every accepted environment is green on compile, test and verify (known flakes rerun green),
every other environment compiles, and every item is marked fixed or not fixed with evidence.

## 4. Report and hand off

Append "Fix results" to the run's `report.md`: per item fixed / not fixed, the stage table before → after,
the `--compare` output, and `git diff --stat`.

Show the user that summary. Commit on the branch and open a pull request only on their go-ahead. The
PR body lists the bumped versions, the items with their causes, and the per-environment evidence; CI then
checks its environments again.
