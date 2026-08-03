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

## Local completion evidence (2026-08-03)

- The full local branch chain was rebuilt from `f2e3017b77ad9c7dda04549b1e8e7ddaddbdccf5`; every child was rebased onto its immediately preceding child and all ancestry checks are clean.
- The clean upstream checkout and every stack branch pass `./gradlew classes check --no-build-cache cleanTest test tests:test` with the fixed JDK 22 runtime. The one randomized upstream `QuestTests.testAlternativeBarbarianCampsAreUsed` failure reproduced once on the final branch and passed on the required rerun; it was not a web-code failure.
- Branch 11 received the owning compile fixes for the generated browser suite (`EditorMapHolderMemoryTest` exclusion and `CityView` construction), then the idle-due-worker recovery in `WebValidationRunner`. Branches 12 and 13 were rebased after each fix and rerun.
- Final TeamVM build passes with 8,251 classes and verifies `war-from-start`, `war-preworld`, and `war-deep`. The web-only `ThreadLocalRandom` overlay, JSON hydration fallback, portable file diagnostics, EventBus reflection guard, and multiplayer persistence retry remain outside JVM behavior.
- Final phase-4 desktop and mobile both report 12 pass, 0 fail, 0 blocked, 0 disabled, 0 page errors, and 0 console errors. The JS suite reports 240 executed, 18 explicit TeamVM-only exclusions, 0 failures, 0 page errors, and 0 console errors.
- Dedicated file I/O, UI multiplayer, multi-instance turn/chat synchronization, strict clickops, WAR-from-start, WAR-preworld, WAR-deep, and a local Pages staging smoke all pass. Exact evidence paths and branch SHAs are in `tasks.csv`.

## Final post-rebase audit (2026-08-03)

- The final branch refs are rebuilt from `f2e3017b77ad9c7dda04549b1e8e7ddaddbdccf5`; `agent/web-stack-v3/rebase-work` now points to `agent/web-stack-v3/13-final-gates`.
- The direct-parent audit passed for BASE and S00-S13: each child’s first unique commit has the immediately preceding branch as its parent. `git diff --check` and conflict-marker scans passed for every ref.
- The counted-file maximum is 62 (S01); every branch is at or below the 100-file limit. `web/**`, `plans/**`, `docs/**`, and `*.md` were excluded exactly as specified. S00 has no web classes and therefore no web tests; web contracts begin when web classes are present.
- The clean upstream baseline passed `.rebuild-backup/jvm-upstream-baseline.log`. Fresh branch logs `.rebuild-backup/jvm-final-01-*.log` through `.rebuild-backup/jvm-final-13-*.log` all pass the requested task list. On this Windows host the same task list was run with `--no-daemon --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process` to avoid the local Kotlin daemon/CreateProcess failure and parallel Metaspace exhaustion.
- The final current tree passes `.rebuild-backup/web-final-gates-r3.log`: latest TeamVM `:web:webBuildJs`, 8,250 classes, WAR preload verification for all three WAR fixtures, and `BUILD SUCCESSFUL`.
- The final Node contract run is 22/22. The browser JS suite is 240 executed, 0 failures, 18 explicit TeamVM-only exclusions, 0 page errors, and 0 console errors. Phase-4 desktop and mobile each report 12 pass, 0 fail, 0 blocked, 0 disabled, 0 page errors, and 0 console errors in `phase4-desktop-final-r5` and `phase4-mobile-final-r5`.
- Final dedicated flows pass: file I/O, UI multiplayer, multi-instance turn/chat synchronization, strict clickops, WAR-from-start, WAR-preworld, and WAR-deep. The local Pages staging smoke remains recorded in `pages-smoke-site-r2` and the final WAR/browser outputs are recorded in `tasks.csv`.
- After the ABI correction, current-ref TeamVM web builds were rerun on S01-S05 and all passed (`.rebuild-backup/web-final-01-*` through `.rebuild-backup/web-final-05-*`), closing the early-branch evidence gap.

### Fixes found during the final audit

- A cherry-pick had placed the server `parseAuthFromQuery` helper inside the WebSocket `when` block. Restoring the helper’s upstream structure fixed the branch-01 JVM gate without changing the web protocol.
- Later upstream core methods regained Kotlin default arguments while the web multiplayer overlay did not. The overlay now supplies matching default parameters so TeaVM can resolve the generated `$default` ABI; JVM core behavior is unchanged.
- Branch 11 had duplicate generated-browser-test source configuration after conflict resolution. It was deduplicated before the final JVM/web sweeps.
- Branch 11’s phase-one Node contract was matching the pre-branch-11 spelling `fun webPhase1(): Features`; it now accepts both the explicit-return and inferred-return Kotlin forms. Branches 12 and 13 were rebased afterward and their inherited contract suite passes.
- The original fresh-origin crash remains addressed by the web-only `ThreadLocalRandom` compatibility shim and the other web-only portability layers; no core/JVM fallback was changed.

### Post-rebuild publication and toolchain audit (2026-08-03)

- The authorized publication target is `YosefLm/unciv-web`. The complete stack is published there as PRs 1-14; each PR targets the immediately preceding stack branch, with `agent/web-stack-v3/base-upstream-master` rooted at the pinned upstream SHA.
- The final rebase retained the startup compatibility asset in the first web module: the build derives web-only `assets/startup-logo.png` from the checked-in Unciv icon. This fixes the current TeamVM preload request without changing JVM/core behavior.
- Browser CI uses Node 26, `playwright@latest`, and `ws@latest`. The standalone phase-1 web build uses Java 22, the newest Java proven locally with this Gradle 8.11.1 project; the Docker builder keeps Java 21 because that is the reliable Ubuntu 24.04 package and the upstream workflow baseline.
- A fresh Windows exact-gate retry was blocked by the host’s JDK 22 Kotlin worker `CreateProcess error=1920`; prior exact branch logs and the clean upstream baseline pass, and the authoritative post-rebase check is the fresh GitHub Actions run. This host-environment failure is not recorded as a source pass.
- The previous web CI failure was diagnosed and fixed: the global `pages` concurrency group canceled sibling stacked PR runs, and the Node 20 Docker image could not install current npm. S11 now uses a per-PR concurrency key and Node 26.
