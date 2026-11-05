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

The initial opt-in module used gdx-teavm 1.6.2 (TeaVM 0.15.0), libGDX 1.14.2,
Kotlin 2.4.20 and Java 17 bytecode. Development validation uses JDK 21.
The backend's published Gradle plugin owns JavaScript compilation, assets and
the generated HTML; there is no custom bundle postprocessor.

Run from the repository root (use `gradlew.bat` on Windows):

```sh
./gradlew -Pweb :web:compileKotlin
./gradlew -Pweb :web:gdx_teavm_web_js_build
./gradlew -Pweb :web:gdx_teavm_web_js_run
./gradlew :tests:test --tests com.unciv.logic.files.SaveLocationTest
```

For the local browser-backend and TeaVM classlib fixes used in the latest
diagnostic run, pass both optional fork paths:

```sh
./gradlew -Pweb \
  -PgdxTeaVMSource=C:/gits/gdx-teavm-unciv \
  -PteavmCompatibilityJar=C:/gits/teavm-unciv/repro/build/compat/teavm-compatibility-0.15.0.jar \
  :web:gdx_teavm_web_js_build
```

The expected web output directory is `web/build/dist/js/webapp`.
Normal builds omit `-Pweb` and do not include the browser module.

Verified so far:

- Unchanged baseline `:core:compileKotlin`: passed.
- Browser module `:web:classes`: passed, including the Kotlin JavaScript bridge.
- `:web:dependencies --configuration teavm` with `-PgdxTeaVMSource=...`: resolved the 1.6.2 `backend-web` module to the local included build.
- `:web:classes` with the local gdx-teavm source commit: passed, compiling `backend-shared` and `backend-web` as part of the composite build.
- `SaveLocationTest`: 3 tests passed, including virtual file listing and all
  eight combinations of local/external save existence and storage preference.
- Independent browser canvas smoke: Latin, Hebrew, Arabic, Vietnamese
  diacritics, emoji and descenders produced visible glyphs within the shared
  font bounds. This checks the bridge script, not the compiled game.

On this Windows host the initial Kotlin daemon build failed deleting a locked
output directory; retrying with `--no-daemon
-Pkotlin.compiler.execution.strategy=in-process` passed.

The first TeaVM generation attempt exceeded its default 1 GiB heap. With a
4 GiB heap and the local 0.15 overlays above, JavaScript analysis completed but
failed with 45 distinct missing-symbol diagnostics (29 classes, 16 methods).
The TeaVM overlay resolved `AtomicReferenceArray`, `LockSupport`,
`ThreadFactory`, and `RejectedExecutionException`; the backend overlay resolved
`Pixmap(ByteBuffer)`. Executor shutdown now reaches a new missing
`ScheduledThreadPoolExecutor` through `kotlinx.coroutines.internal.ConcurrentKt`
and Unciv's dispatcher cleanup. See [COMPATIBILITY.md](COMPATIBILITY.md) for
the captured log, baseline comparison, remaining symbols, and owning layers.
No playable browser bundle has been produced or validated.
Successful Kotlin compilation does not establish a working browser game.

Those 0.15.0 results are historical. The latest tagged gdx-teavm release is
1.6.2 and pins TeaVM 0.15.0, while current upstream gdx-teavm source at
`5e3db3d630d92056f1de14b6f25b8ab84ad48976` pins TeaVM 0.16.0. The next clean
compiler baseline must include that exact gdx source (backend and Gradle
plugin) and use only published TeaVM 0.16.0 modules. The previous experimental
`-PteavmClasslibJar` task-classpath replacement did not demonstrate that the
forked classlib was used and is not a supported workaround.

## Dependency work

The TeaVM 0.15.0 local fork has separate commits for `AtomicReferenceArray`
(`39ac18624`) and executor/parking support (`38d6266bd`). Its compatibility
overlay JAR is generated by `./gradlew -p repro teavmCompatibilityJar` from
`C:/gits/teavm-unciv`; the current artifact SHA-256 is
`0DBBCF56964F7C235F1C7DB5D3BB26854AAFE29CA2C3F7B2AACEE0F37AB63AED`.
The artifact excludes the pending TimeUnit implementation. The standalone
AtomicReferenceArray contract passed against the JDK and generated JavaScript;
the local overlay now resolves the atomic-array, LockSupport, and executor
symbols observed on the initial TeaVM run. Remaining symbols include a
`ScheduledThreadPoolExecutor` reference from coroutine dispatcher cleanup.
These results do not establish Wasm/native correctness or complete coroutine
compatibility.

The gdx-teavm 1.6.2 local fork has a separately committed `Pixmap(ByteBuffer)`
fix (`813eaa38`), with three passing tests for slice selection, position/limit
preservation, bounds and direct-buffer rejection. It reuses the existing image
decoder; browser image decoding has not been independently exercised.

Neither local dependency fix is part of the pinned published artifacts. Use
the explicit fork-build options documented in [COMPATIBILITY.md](COMPATIBILITY.md)
when validating them. The default build remains pinned to published
dependencies until those fixes are published and pinned.

## Independently reviewable core fix

Save lookup now checks `Files.isExternalStorageAvailable` before requesting an
external handle, and logs the chosen handle's path without demanding a native
`java.io.File`. The browser backend explicitly rejects both external handles
and conversion to native files. Selection rules for available external storage
are unchanged and covered by `SaveLocationTest`.
