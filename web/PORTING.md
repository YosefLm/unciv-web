# Browser port revival

## Current status

The browser build runs vanilla single-player in headless Chrome with SwiftShader.
Verified: English picker, menu, Tiny Vanilla new game, map, unit movement, founding
a city, choosing research and production, ending a turn, saving, reloading the
page, and loading the named save. The final fresh Babylon game retained turn 1,
gold 3, Animal Husbandry research, Monument production and the same map after
reload. Its exported save contains nonzero explored-region coordinates.

The save-list preview required two additional reflection registrations. The
generated-JavaScript regression and final browser preview/load check both pass.
The backend still reports zero file modification time, so save dates show 1970;
this does not prevent loading, but date display/sorting needs follow-up.

The full JavaScript bundle was built from Unciv `ac18ff19b`, TeaVM fork
`fee13672c` (TeaVM 0.16.0 based), and gdx-teavm fork `8444685af`. The compiler
log is `web/build/diagnostics/teavm-js-0.16-preview-reflection-fix.log` (4m 44s).
The generated `app.js` is 74,819,768 bytes, SHA-256
`C458EC7A9F3C168F14EFAE59938350E5DA0FD804C9C427BF21AF8F833CC3C9D1`.
Exact build details and prior checkpoints are in
[COMPATIBILITY.md](COMPATIBILITY.md).

Baseline: Unciv `608d9de26b16f834d5fed19df4fd1bd65bfcbf3e`; old
[PR #14709](https://github.com/yairm210/Unciv/pull/14709) is the reference.
The shared ancestor has 1,070 intervening upstream commits. Core changes are
14 files, 88 added lines and 27 removed lines; most implementation is in the
opt-in web module and the two dependency forks.

## Scope and constraints

The first gameplay target is vanilla single-player in a browser. Multiplayer,
mods, sound, and WebAssembly remain separate follow-up stages. Named save writing and loading after a page reload are demonstrated.

- Keep web support opt-in; normal builds must not acquire local source overrides.
- Use current Unciv APIs and preserve ordinary initialization and disposal.
- Fix reusable JVM/API gaps in TeaVM and browser behavior in gdx-teavm. Keep
  Unciv-specific changes narrow and tied to demonstrated platform needs.
- Do not use mutable snapshots, counterfeit test classes in production, fake
  synchronous coroutine dispatchers, blanket retries, or swallowed startup errors.
- Add reflection metadata from actual JSON and runtime use, not a broad package
  preservation list.

## Build and run

The opt-in module targets libGDX 1.14.2, Kotlin 2.4.20 and Java 17 bytecode.
The tested plugin/backend source fork pins gdx-teavm 1.7.0 with TeaVM 0.16.0.
Both source forks are required for web build and run tasks. From the repository
root, substitute the local absolute paths as needed:

```powershell
.\gradlew.bat -Pweb `
  -PgdxTeaVMSource=C:\gits\gdx-teavm-unciv `
  -PincludeTeaVMSource=true `
  -PteavmPath=C:\gits\teavm-unciv `
  -PwebCompilerMemory=8192 `
  :web:compileKotlin

.\gradlew.bat -Pweb `
  -PgdxTeaVMSource=C:\gits\gdx-teavm-unciv `
  -PincludeTeaVMSource=true `
  -PteavmPath=C:\gits\teavm-unciv `
  -PwebCompilerMemory=8192 `
  :web:gdx_teavm_web_js_build

.\gradlew.bat -Pweb `
  -PgdxTeaVMSource=C:\gits\gdx-teavm-unciv `
  -PincludeTeaVMSource=true `
  -PteavmPath=C:\gits\teavm-unciv `
  -PwebCompilerMemory=8192 `
  :web:gdx_teavm_web_js_run
```

`webCompilerMemory` is the TeaVM worker heap size in MB. The full game currently
needs 8192 MB for compilation. The expected output is
`web/build/dist/js/webapp`. Do not combine TeaVM versions. Normal builds omit
`-Pweb` and keep existing plugin repository behavior.

## Validation boundaries

- The current full compiler build and browser sequence above pass.
- The browser test demonstrates one turn and a named save/load across page reload.
  It does not establish long-game stability or support across browsers.
- JVM and generated-JavaScript JSON contracts, TeaVM core tests, Fetch contracts,
  executor/input suspension, reflection, and browser runtime regressions are
  recorded in [COMPATIBILITY.md](COMPATIBILITY.md).
- Successful compilation alone does not establish gameplay. Conversely, the
  current browser run proves the listed path only; it does not claim multiplayer,
  arbitrary mods, or broad compatibility.

## Review and follow-up sequence

The implementation is split locally by owning repository. Review reusable TeaVM
changes first (class-library contracts and optimizer regressions), then the three
gdx-teavm fixes (Pixmap buffer semantics, render callback suspension, queued input).
Review Unciv's save-path/platform seams separately from the opt-in web module,
launcher, Fetch engine and reflection registrations. Local commits preserve those
boundaries; no remote branch or pull request has been published.

The local branches are `codex/web-revival` (this worktree),
`codex/unciv-teavm-0.16` (`C:/gits/teavm-unciv`) and
`codex/pixmap-teavm-0.16` (`C:/gits/gdx-teavm-unciv`). The shared agent workflow is
`C:/Users/yosef/.codex/skills/unciv-teavm-port/SKILL.md`.

Before broad release, validate longer games, browser lifecycle/backgrounding,
more browsers and hardware, and bundle size/performance. Multiplayer, mods,
file sharing, audio and WebAssembly need separate implementation/validation.
The current milestone is the observed minimal vanilla gameplay path.

## Historical work

The old PR #14709 implementation is a reference, not the base of this port. The
revival starts from current Unciv upstream and preserves the useful split between
game changes, browser backend changes, and reusable TeaVM fixes. Earlier 0.15
diagnostics, intermediate 0.16 compiler failures, and resolved startup/runtime
errors are retained as historical evidence in [COMPATIBILITY.md](COMPATIBILITY.md).
