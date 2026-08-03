# T13 — Rebuild the Unciv web stack from upstream master

## Objective

Rebuild the web stack from upstream `yairm210/Unciv` master, reproduce PR #14709 first, then retain the existing feature slices in dependency order, with TeaVM web support as the final segment. Every segment must be independently buildable, testable, and mergeable into its immediately preceding branch.

## Immutable baseline and publication scope

- Upstream repository: `yairm210/Unciv`
- Regression baseline: `master` at `f2e3017b77ad9c7dda04549b1e8e7ddaddbdccf5`
- Original source reference: PR #14709, change `969b343b16ea99c7c3d924a05ec486cc99c5431f`
- Local repository: `C:/gits/unciv-web-stack-redo`
- Allowed GitHub owner: `haimlm/*` only
- No branch, remote, fetch, push, or PR may use a repository outside `haimlm/*`.

The old `51c07b743` base is historical context only and is not a regression baseline for this rebuild.

## TeamVM snapshot

The web build uses the latest locally resolved TeamVM/GDX snapshot available during this rebuild:

- TeaVM `0.15.0`
- `backend-web:-SNAPSHOT` cache hash `fa8a556056b23c543ddbd2a21d48ec99303021f1`
- `backend-shared:-SNAPSHOT` cache hash `84d2f38e55e1ab17c1b8c5e6814ab437f110075b`
- `gdx-freetype-web:-SNAPSHOT` cache hash `aceeb5dca6fdb19adf6df7095152554d12dcb478`
- GDX `1.14.0`
- web coroutines compatibility pin `1.8.1`

The exact resolved source SHA and these snapshot identifiers are recorded for every branch in `tasks.csv`.

## Branch chain

Each child targets the immediately preceding branch:

1. `agent/web-stack-v3/base-upstream-master` — upstream master baseline
2. `agent/web-stack-v3/00-tooling-ci`
3. `agent/web-stack-v3/01-web-module-bootstrap`
4. `agent/web-stack-v3/02-phase1-platform-core`
5. `agent/web-stack-v3/03-phase1-playable-runtime`
6. `agent/web-stack-v3/04-phase1-smoke-ci`
7. `agent/web-stack-v3/05-core-followup-concurrency`
8. `agent/web-stack-v3/06-core-followup-appclipboard`
9. `agent/web-stack-v3/07-file-io`
10. `agent/web-stack-v3/08-mod-downloads`
11. `agent/web-stack-v3/09-multiplayer`
12. `agent/web-stack-v3/10-audio`
13. `agent/web-stack-v3/11-deep-validation-and-pages`
14. `agent/web-stack-v3/12-teavm-compat`
15. `agent/web-stack-v3/13-final-gates`

The first twelve feature slices reproduce the source work in small dependency-owning increments. Segment 12 contains the web-only runtime compatibility and the strict browser fixes. Segment 13 contains the enforcement/documentation needed to keep the stack reproducible and mergeable.

## Required gate for every branch

For each branch, record the parent and head SHA, source baseline SHA, TeamVM snapshot, changed-file counts, and all results in `tasks.csv`.

Run:

```text
./gradlew classes check --no-build-cache cleanTest test tests:test
```

against both the branch and a clean checkout of upstream master at the same source SHA. Also run:

- `git diff --check`
- conflict-marker scan
- parent ancestry check
- `node scripts/stack/check-stack-size.js --base-ref <parent> --head-ref <head>`
- the branch’s own focused test and all inherited focused tests

No missing, blocked, informational, or skipped result is a passing gate.

## Web-only gates

On web-enabled branches use the TeamVM snapshot above and run:

- `:web:webBuildJs`
- WAR preload verification for `war-from-start`, `war-preworld`, and `war-deep`
- Node contract suite
- browser JS suite
- phase-1 and phase-4 browser profiles
- strict multiplayer clickops, multi-instance, UI multiplayer, file/mod flows, audio, clipboard, and local Pages smoke

The web-only compatibility layer must not alter JVM/core behavior. It includes the `ThreadLocalRandom` shim, platform overlays, portable web JSON hydration, URL/deep-link handling, TeamVM visibility lifecycle handling, web file-storage retry/path keys, and the EventBus reflection guard. Fresh-origin startup, new-game start, world entry, actions, turn sync, WAR preload, and chat must have zero crash screens, `UnsupportedOperationException`, page errors, console errors, and JavaScript test failures.

## Known crash and resolution

The original fresh-origin crash was first caused by `FileHandle.file()` in a web startup diagnostic. After that was removed, TeamVM’s GDX JSON path loaded technology columns without nested technology objects, leaving the ruleset technology map empty and causing strategic-resource placement to dereference a missing technology. The web parser hydration fallback repairs that data without changing JVM parsing.

The strict multiplayer flow then exposed two more web-only failures: a local IndexedDB/FileHandle identity race and a Kotlin reflection call in `EventBus.getSuperClasses`. The web overlay retries local preview persistence with a JavaScript `setTimeout`, keys local multiplayer previews by stable path, and the EventBus catches unavailable Kotlin reflection while retaining direct event listeners. These paths are covered by strict browser clickops and web contracts.

## Completion criteria

Only mark a segment mergeable after its own gate and every inherited gate pass. The final segment is complete only when the exact JVM branch-vs-clean-upstream regression suite, all web gates, all six final phase-4 flows, file/mod behavior, multiplayer behavior, WAR-from-start/preworld/deep, and Pages smoke are recorded as passing in `tasks.csv`.
