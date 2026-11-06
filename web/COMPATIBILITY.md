# TeaVM and browser compatibility

This page separates validated current results from earlier compiler and runtime
checkpoints. A successful JavaScript compile and a browser gameplay run are
distinct checks; the minimal gameplay/save/reload sequence now passes.

## Current validated results

### Full build and gameplay

The latest completed full game build used Unciv `ac18ff19b`, TeaVM fork
`fee13672c` based on TeaVM 0.16.0, and gdx-teavm fork `8444685af`. It completed
in 4m 44s. Log:
`web/build/diagnostics/teavm-js-0.16-preview-reflection-fix.log`.

The generated `web/build/dist/js/webapp/app.js` was 74,819,768 bytes with SHA-256
`C458EC7A9F3C168F14EFAE59938350E5DA0FD804C9C427BF21AF8F833CC3C9D1`.
The preceding bundle launched in headless Chrome with SwiftShader and passed this vanilla
single-player path: English picker, main menu, Tiny Vanilla England new game,
map display, Warrior move from MP 2 to 1, found London, select Animal
Husbandry research, queue a Monument, and end turn 1. It also wrote a named save.

### Save and reload validation

Unciv `f05dc7d08` adds targeted metadata for serialized `Vector2` fields and the
`GameInfoSerializationVersion` fallback parser. JVM and generated-JavaScript
contracts passed against the captured 44 KB save and a nonzero-coordinate variant.

On the rebuilt bundle a fresh Tiny Vanilla game was created as Babylon, the city
founded, Animal Husbandry selected, Monument queued, and turn 1 completed. The
named save `revival-final-babylon-turn1` was written, the page reloaded, and that
save loaded through the UI. The map, Babylon city, turn 1 / 3,940 BC, gold 3,
research (5 turns remaining), and Monument (5 turns remaining) matched.

Read-only IndexedDB export confirms game ID
`f700883a-ac70-41cf-b1b3-a45cf8e11e74`, 43,356 bytes, and explored-region bounds
`topLeft=(-5,3)`, `bottomRight=(-9,-5)`. Screenshot evidence is in the ignored
local diagnostics directory `web/build/validation/headless-browser`:
`gameplay-11-final-turn-one.png` and `gameplay-17-final-loaded.png`.
The browser reported no page errors, failed requests or HTTP error responses.

The save-selection preview initially showed an incorrect error label while actual
loading succeeded. Unciv `ac18ff19b` registers `GameInfoPreview` and
`CivilizationInfoPreview`. The generated-JS regression fails without those rules
and passes with them, including ID migration, human-player filtering and date
formatting. The final full-app build passes (4m 44s); browser preview now displays Babylon,
Prince, turn 1, and Civ V - Vanilla correctly, and the game loads again. Evidence:
`gameplay-02-preview-fixed.png`, `gameplay-03-verified-final-bundle.png`.

Known backend limitation: `WebFileHandle.lastModified()` currently always returns
zero, so the preview displays 1970 rather than the IndexedDB record date. Date
display/sorting remains a follow-up; the named save and its actual contents persist.

## Reproducing the web build

The opt-in web module uses libGDX 1.14.2, Kotlin 2.4.20 and Java 17 bytecode.
The tested gdx-teavm source fork pins plugin/backend 1.7.0 with TeaVM 0.16.0.
Both source forks are required for web compilation and running. From the Unciv
repository root, use the same arguments for compile, JavaScript build, and run:

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

Replace those absolute paths with the local fork checkouts if they differ.
`webCompilerMemory` is the TeaVM worker heap in MB; the full app currently needs
8192 MB. Output is under `web/build/dist/js/webapp`. Avoid mixing TeaVM versions.
Normal builds without `-Pweb` do not use these source composites.

## Current compatibility changes

- TeaVM 0.16 fork fixes cover concurrency used by coroutines, executor and
  scheduled-executor behavior, `LockSupport`, blocking queues/read-write locks,
  locale/collation, SHA-1, secure random, file and classloader APIs, and native
  JavaScript throwable stack capture. The source fork has focused JVM and
  generated-JavaScript contracts for these changes.
- TeaVM optimizer fixes cover dominator computation with unreachable
  predecessors and avoid loop-invariant code motion across external exception
  entries. TeaVM core tests and Checkstyle passed for those fixes.
- gdx-teavm fixes cover the libGDX `Pixmap(ByteBuffer)` constructor and web input
  dispatch. DOM defaults/focus handling stays synchronous; event state and
  callbacks run in FIFO order on the persistent render thread. The browser
  regression checks suspended frames, default prevention, input state and thread
  identity.
- Unciv's web reflection policy is narrow and driven by observed JSON/runtime
  requirements. It covers save-marker types, ruleset models, selected settings
  and skin types, scene2d defaults, file texture pixmap access, and enum
  annotation lookup. The latest targeted additions for Vector2 and the fallback
  version parser passed the fixture noted above.
- The browser Fetch engine handles HTTP requests without exposing CIO sockets to
  TeaVM. Focused tests cover UTF-8 streaming, status/headers, error bodies,
  network rejection, request and response cancellation, and request timeout.

These source changes are local forks, not published upstream releases. Keep
compiler/backend/classlib artifacts paired to the same fork revisions when
reproducing results; source substitution is configured with the two source
properties above, not by adding an arbitrary replacement JAR to a task.

## Additional validation evidence

- All 217 TeaVM core tests and applicable Checkstyle checks passed.
- SaveLocationTest passed 3 tests, including eight local/external save combinations.
- JVM loopback tests verified default CIO selection and raw WebSocket ping/pong.
- Generated JavaScript contracts exercised real settings, saves, rulesets and skins.
- Current relocated source artifacts, SHA-256:
  - core: `684AF8FF9A94EE88A7335EB02EA20F6C4A2B00563B806BF4CC30659467627BF0`
  - classlib: `CD4021DC1BA708EA47D0185D32B802795BBB90FBE18921E274252995446314AF`
  - JSO APIs: `7E0E919BD0F1D608A97858C05C2A32F4FD38756583153E3D1C2596559FFC9CEE`

## Historical compiler and runtime checkpoints

The entries below document resolved investigations and older baselines. They are
not current blockers unless called out in the current status section.

### Initial TeaVM 0.16 diagnostics

The published 0.16 baseline reported 48 unique missing symbols (32 classes and
16 methods). The initial TeaVM/gdx-teavm source substitution reduced this to 42
(29 classes, 13 methods) and exposed a scheduled-executor gap. Those counts are
compiler diagnostics from before the compatibility fixes summarized above. Logs:

- `web/build/diagnostics/teavm-js-0.16-published-baseline.log`
- `web/build/diagnostics/teavm-js-0.16-fork-combined.log`

Earlier TeaVM 0.15 work reported 49 missing symbols, later 45 after an overlay.
That overlay and its task-level JAR replacement experiments are obsolete; current
web builds use the TeaVM 0.16 source composite.

### Resolved browser failures

- **Suspending from an animation callback:** the first bundle stayed black when
  font initialization reached coroutine `runBlocking` from a JavaScript callback.
  gdx-teavm `9ac36c5` moved animation work to a persistent render executor. The
  focused browser test proved the prior direct-callback path failed and the
  executor path preserves serial frames and Java thread identity.
- **DecimalFormat omitted suffix:** startup error rendering found a null negative
  suffix. TeaVM `cd3eddd9b` represents an omitted suffix as empty while retaining
  explicit-null setter behavior; JVM and generated-JS affix contracts passed.
- **Unmatched regex capture:** language-picker keyboard labels used replacement
  `$1$4 $2$3$5` with optional groups. TeaVM `fee13672c` maps unmatched captures to
  empty text both in initial replacement parsing and cached replacement parts.
  Alternating matches, no-match input and `appendReplacement` passed JVM and JS
  contracts; the prior implementation failed.
- **Serialized scene/game data:** start-game JSON loading exposed missing
  `FileTextureData.pixmap` metadata. gdx-teavm/Unciv metadata was restricted to
  actual fields and constructors instead of preserving whole packages. The
  latest new-game run passed; save reload passes after the targeted metadata additions above.
- **Throwable stacks:** coroutine stack recovery initially received an empty JS
  stack. TeaVM `76beab7ed` captures native JS frames and preserves writable-stack
  semantics; JVM, generated-JS, print-only contracts and Checkstyle passed.
- **Compiler optimizer:** the first full analysis exhausted 4 GB and needed an
  8 GB TeaVM worker. It then exposed a dominator error from an unreachable
  predecessor, followed by an unreachable preheader bug around exception edges.
  TeaVM fixes `f2de7ba8c` and `5757a02e8` pass their focused regressions and the
  full game compile. Historical logs include
  `teavm-js-0.16-json-stack-8g.log`,
  `teavm-js-0.16-optimizer-stacktrace.log`, and
  `teavm-js-0.16-dominator-fix.log`.

### Earlier browser caveats

The initial headless launch logged invalid `glTexParameterf` magnification-filter
enums from atlas metadata. The same filter was supplied by the existing atlas
configuration; it was not the cause of the black canvas or later startup errors.
That warning is separate from the validated gameplay path.
