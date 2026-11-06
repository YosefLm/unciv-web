# Browser port revival

## Baseline and scope

This integration starts from Unciv `608d9de26` (upstream master fetched November 5,
2026), not from the old web fork. Reference implementation:
[PR #14709](https://github.com/yairm210/Unciv/pull/14709), head
`92b9f9b64fd2ae39484b9a5bf44f0cc8cd362b1a`. Upstream has 1,070 commits since
the shared ancestor `fff82fd49f832e2a87f7d9a9d1bce15ff5115eb0`.

The first gameplay target is vanilla single-player in a browser: launch, create
a game, display the map, move a unit, end a turn, and save/reload. Multiplayer,
mods, file pickers, sound, and WebAssembly are separate follow-up stages.

## Implementation sequence

1. Add an opt-in web build with pinned dependencies, using current Unciv's
   Kotlin and libGDX versions. Establish the unchanged core build baseline.
2. Adapt the old launcher and browser font/display integration to current APIs.
   Keep normal Unciv initialization and disposal behavior.
3. Compile to JavaScript and reduce compatibility failures to reproductions.
   Fix generic JVM/class-library issues in TeaVM; fix libGDX browser behavior
   in gdx-teavm. Maintain a local TeaVM fork when an upstream fix is needed.
4. Add only demonstrated Unciv platform seams. Explain each core change and
   validate its desktop behavior. Do not shadow whole game subsystems.
5. Validate the actual browser gameplay sequence above, including persistence
   and JSON round trips. Split the resulting changes by owning layer for review.

## Constraints from the previous implementation

- No mutable dependency snapshots or implicit local Maven overrides.
- No test source trees or counterfeit JUnit/Hamcrest classes in production.
- No synchronous coroutine dispatcher masquerading as a thread pool.
- No blanket startup retries, swallowed initialization errors, or disposal
  overrides that bypass normal cleanup.
- Register serialization reflection metadata from actual requirements; do not
  transplant the old giant preservation list without validation.
- Keep Java-only bridge code limited to demonstrated interop requirements;
  prefer Kotlin for the launcher and integration.

## Validation status

The opt-in module uses libGDX 1.14.2, Kotlin 2.4.20 and Java 17 bytecode.
gdx-teavm 1.6.2 is the latest tagged release and pins TeaVM 0.15.0, so web
builds require a gdx-teavm source checkout whose version catalog pins
gdx-teavm 1.7.0 / TeaVM 0.16.0. The same checkout supplies the Gradle plugin
and backend. Development validation uses JDK 21. The backend plugin owns
JavaScript compilation, asset handling and generated HTML.

Run from the repository root (use `gradlew.bat` on Windows):

```sh
./gradlew -Pweb -PgdxTeaVMSource=/path/to/gdx-teavm-0.16 :web:compileKotlin
./gradlew -Pweb -PgdxTeaVMSource=/path/to/gdx-teavm-0.16 :web:gdx_teavm_web_js_build
./gradlew -Pweb -PgdxTeaVMSource=/path/to/gdx-teavm-0.16 :web:gdx_teavm_web_js_run
./gradlew :tests:test --tests com.unciv.logic.files.SaveLocationTest
```

To include the current TeaVM fork for compatibility work, add its source
composite properties:

```sh
./gradlew -Pweb \
  -PgdxTeaVMSource=/path/to/gdx-teavm-0.16 \
  -PincludeTeaVMSource=true \
  -PteavmPath=/path/to/teavm-0.16-fork \
  -PwebCompilerMemory=8192 \
  :web:gdx_teavm_web_js_build
```

The expected web output directory is `web/build/dist/js/webapp`.
`webCompilerMemory` sets the TeaVM worker heap in MB (default 4096). The current
full-game analysis with JSON reflection metadata needs the 8192 MB setting above.
Normal builds omit `-Pweb` and do not include the browser module or alter the
existing local Maven repository behavior.

Verified so far:

- The full JavaScript build succeeds with TeaVM fork `5757a02e8` and gdx-teavm
  fork `19c58e88fd0b9fd8c7f8d710efbfa31bff4a9617`, using the 8192 MB compiler heap.
  The output bundle and assets are packaged; browser gameplay validation is still
  pending. Earlier compiler checkpoints below are historical; see
  [COMPATIBILITY.md](COMPATIBILITY.md) for current artifact hashes and the fixes.
- All 217 TeaVM core tests pass, including regressions for dominator calculation
  and loop motion across exceptional entries.
- JVM and generated-JS JSON contracts pass for real Unciv settings, saves,
  rulesets and skins. The focused Fetch contract passes UTF-8 streaming,
  request/response cancellation, network errors and request timeout handling.

- Unchanged baseline `:core:compileKotlin`: passed.
- `:web:classes` passed with the source plugin/backend at gdx-teavm upstream
  commit `5e3db3d630d92056f1de14b6f25b8ab84ad48976`.
- The clean published TeaVM 0.16.0 baseline reported 48 unresolved symbols
  (32 classes, 16 methods). Log and details are in
  [COMPATIBILITY.md](COMPATIBILITY.md).
- Source substitution with gdx-teavm commit
  `19c58e88fd0b9fd8c7f8d710efbfa31bff4a9617` and TeaVM commit
  `5c6b80cb6f9b8884cea99cfa9b063131992f10ab` completed JS analysis with 42
  unresolved symbols (29 classes, 13 methods). The source build removed seven
  baseline diagnostics and exposed one scheduled-executor gap. It did not
  produce a validated browser game.
- `SaveLocationTest`: 3 tests passed, including virtual file listing and all
  eight combinations of local/external save existence and storage preference.
- Independent browser canvas smoke: Latin, Hebrew, Arabic, Vietnamese
  diacritics, emoji and descenders produced visible glyphs within the shared
  font bounds. This checks the bridge script, not the compiled game.

On this Windows host the initial Kotlin daemon build failed deleting a locked
output directory; retrying with `--no-daemon
-Pkotlin.compiler.execution.strategy=in-process` passed.

The first browser launch of the compiled bundle stays on a black canvas; startup diagnostics are in progress. Menu, gameplay and persistence have not been validated.
Successful Kotlin compilation does not establish a working browser game.

The earlier 0.15.0 overlay and task-level JAR replacement experiments are
historical only. The JAR replacement did not demonstrate that the TeaVM fork
classlib was actually used. Current fork validation uses Gradle composite
dependency substitution instead; see [COMPATIBILITY.md](COMPATIBILITY.md).

## Dependency work

The current TeaVM fork is based on 0.16.0 and has passing standalone JVM and
generated-JavaScript contracts for its class-library changes. Its source build
is selected through `-PincludeTeaVMSource=true -PteavmPath=...`, not by adding
an arbitrary JAR to a task classpath. The gdx-teavm Pixmap constructor fix is
on the 0.16-compatible backend branch and its backend tests pass. These source
changes remain experimental and are not published releases.

## Independently reviewable core fix

Save lookup now checks `Files.isExternalStorageAvailable` before requesting an
external handle, and logs the chosen handle's path without demanding a native
`java.io.File`. The browser backend explicitly rejects both external handles
and conversion to native files. Selection rules for available external storage
are unchanged and covered by `SaveLocationTest`.
