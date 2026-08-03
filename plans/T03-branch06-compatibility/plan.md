# T03 — Branch 06 compatibility and independent compilation

## Goal

Keep `agent/web-stack-v2/06-core-followup-appclipboard` independently compilable while preserving the feature-by-feature stack order and the latest TeaVM snapshot API introduced in branch 01.

## Baseline

- Parent: `agent/web-stack-v2/05-core-followup-concurrency`
- Source feature branch: `origin/web/stack/06-core-followup-appclipboard`
- Source lineage base: `51c07b743`
- Publication scope: `haimlm/*` only

## Compatibility changes

1. Add the web-safe `UncivGame.requestExit()` API required by `CrashScreen`.
2. Add the earliest owning `AsyncAuthProvider` interface required by the multiplayer UI.
3. Preserve upstream-compatible default arguments for multiplayer resign/skip calls.
4. Remove the premature branch-06-only `WorldMapHolder.ensureInteractionState()` call; that dependency belongs to a later feature layer.
5. Keep the clipboard contract test in branch 06 so the branch tests its own feature.

## Verification

- Run the branch-06 Node contract test.
- Run `:web:compileJava` with the current Gradle/TeaVM snapshot configuration.
- Commit only branch-06 compatibility and test changes.
- Rebase branches 07–11 onto the corrected branch-06 commit with `git rebase --onto`.

## Completion evidence

Record the compile command, test result, commit, and any remaining blocker in `tasks.csv`.
