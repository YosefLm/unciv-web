# TeaVM compatibility status

This file records JavaScript compiler passes against the current Unciv baseline, both with published dependencies and with explicit local TeaVM/gdx-teavm source overrides. It is a compiler compatibility report, not evidence that the browser port launches or is playable.

**Current version:** the latest tagged gdx-teavm release is `1.6.2` and pins TeaVM `0.15.0`. The current upstream source at `5e3db3d630d92056f1de14b6f25b8ab84ad48976` identifies itself as `1.7.0-SNAPSHOT` and pins TeaVM `0.16.0`. Since no tagged backend release supports 0.16.0, `-Pweb` now requires an explicit source checkout whose version catalog pins gdx-teavm 1.7.0 and TeaVM 0.16.0. It includes that checkout's Gradle plugin and backend. Normal builds without `-Pweb` retain the existing plugin repository behavior. Do not combine TeaVM versions.

## Current successful build

The full JavaScript build succeeded on November 6 with Unciv code at `46f522db1`,
TeaVM fork `5757a02e8` (based on release 0.16.0), and gdx-teavm fork
`19c58e88fd0b9fd8c7f8d710efbfa31bff4a9617`. Command:

```sh
./gradlew -Pweb -PgdxTeaVMSource=/path/to/gdx-teavm-unciv \
  -PincludeTeaVMSource=true -PteavmPath=/path/to/teavm-unciv \
  -PwebCompilerMemory=8192 :web:gdx_teavm_web_js_build
```

Log: `web/build/diagnostics/teavm-js-0.16-licm-fix.log` (5m 9s).
The packaged `web/build/dist/js/webapp/app.js` is 74,559,060 bytes, with assets,
HTML and supporting runtime files. This is a successful build; browser gameplay
validation is a separate, pending milestone.

Selected source-built artifact SHA-256 values:

- core: `684AF8FF9A94EE88A7335EB02EA20F6C4A2B00563B806BF4CC30659467627BF0`
- classlib: `F06D7ACA51BC48149ABFE33F0DE1D882AA6098408F2C0A8F04CEF8041B3E9F69`
- JSO APIs: `7E0E919BD0F1D608A97858C05C2A32F4FD38756583153E3D1C2596559FFC9CEE`

Validation includes all 217 TeaVM core tests, the focused generated-JS class-library
contracts, real Unciv JSON data contracts, Fetch streaming/cancellation tests, and
a JVM loopback test of default CIO selection and raw WebSocket ping/pong handling.

## Reproduction and intermediate checkpoints

Earlier working-tree checkpoint (November 6): the full source-fork JavaScript pass reported
**four** unresolved symbols: `MessageDigest`, `Collator`, `Locale.forLanguageTag`, and
`SecureRandom.getInstanceStrong`. The log is
`web/build/diagnostics/teavm-js-0.16-platform-http.log`. This pass includes the browser HTTP
engine selection, web-only CIO exclusion, nullable GC metric, and the reusable TeaVM
concurrency/resource/file/Base64 fixes. The selected relocated classlib jar SHA-256 is
`328719B445D514C100E74E32354B9C1E891D6BFBAC134B86D699BC95C79354A5`.
The original classlib jar is a separate task output and was older: source composite builds
run `relocateJar`, so do not assume `*-original.jar` is the artifact just used by the game.
At this checkpoint Fetch cancellation still failed at runtime. Subsequent focused results
are recorded below. Browser gameplay remains unverified. The 42-symbol checkpoint below is older.

Subsequent focused validation implemented all four symbols above in the TeaVM fork,
before the successful full-game build recorded above. The production web
reflection policy passes `web/validation/json-roundtrip` on both the JVM and generated
JavaScript, including private settings fields, a non-default orientation enum, nested
save data and Unicode collections, inherited ruleset stats, and skin colors/variants.
The JavaScript fixture used these copied SHA-256 artifacts:

- classlib: `CE2D3369A4FD8519C8D33AAA4CD1247AEDA64C6E20438B5AC0997684CE184BE4`
- core: `389E92DDE87F90204F783C71FD784E8B84B1C32D41A03E8863BF6CDB37FD5EEB`
- JSO APIs: `7E0E919BD0F1D608A97858C05C2A32F4FD38756583153E3D1C2596559FFC9CEE`

These precede the native JavaScript stack capture patch, committed in the TeaVM fork as
`76beab7ed`. Its JVM reference, generated-JS stack contract, print-only JS contract, and
Checkstyle passed. This resolves the empty Throwable stack encountered by coroutine recovery.
The full Fetch fixture then passed against immutable copied artifacts: core
`8D86500720FEAFA9275412C0E74678E9DE652974F0ED9351C1DA1C418A20A4CE` and classlib
`F06D7ACA51BC48149ABFE33F0DE1D882AA6098408F2C0A8F04CEF8041B3E9F69`.
It verifies UTF-8 GET/POST bodies, streaming across a multibyte-character boundary,
status/headers, HTTP error bodies, network rejection, request abort, response-reader
cancellation, and request timeout. The browser stream-reader cancellation required an
explicit `ByteReadChannel.cancel` bridge; cancellation is not disabled in the test.

The full-game build with reflection metadata saturated the default 4096 MB compiler heap
during dependency analysis. That attempt was stopped and repeated with
`-PwebCompilerMemory=8192`; this property controls the TeaVM worker heap in MB.
The 8192 MB run completed dependency analysis without the previous missing-API
diagnostics, then failed while optimizing `WorldScreen$nextTurn$1.invokeSuspend`.
The repeated run with `--stacktrace` identifies `DominatorTreeBuilder.build:61`,
called by `RepeatedFieldReadElimination.optimize:67`: an unreachable predecessor
leaves a semidominator index of -1. Logs are
`web/build/diagnostics/teavm-js-0.16-json-stack-8g.log` and
`web/build/diagnostics/teavm-js-0.16-optimizer-stacktrace.log`.
At that checkpoint, the compiler failure prevented a usable browser bundle.

The generic dominator fix (`f2de7ba8c`) passes its regression: the old compiler
crashes on `0 -> 1` with an unreachable `2 -> 1`, while the patched compiler
correctly reports 0 as node 1's immediate dominator. The next full-game pass gets
past that crash, then fails with `Variable used before definition: @122 at $39`
in `PhiUpdater`, called by redundant-null-check elimination. Its IR reveals an
unreachable preheader introduced by loop-invariant motion for an exception-handler
loop: normal branches are redirected, but exception edges still target the old
header. Log: `web/build/diagnostics/teavm-js-0.16-dominator-fix.log`.

TeaVM commit `5757a02e8` guards loop motion across external exception entries while
preserving ordinary loop motion and internal exception backedges. The exceptional-entry
regression fails with the guard disabled and passes with it enabled. All 217 core tests
and Checkstyle pass. The rebuilt relocated core SHA-256 is
`684AF8FF9A94EE88A7335EB02EA20F6C4A2B00563B806BF4CC30659467627BF0`.
The next full build succeeded, as recorded in `web/build/diagnostics/teavm-js-0.16-licm-fix.log`.

- Unciv baseline: `608d9de26b16f834d5fed19df4fd1bd65bfcbf3e` (`origin/master`).
- Web builds require `-Pweb` and an absolute `-PgdxTeaVMSource` checkout whose version catalog pins gdx-teavm 1.7.0 / TeaVM 0.16.0. The plugin and backend are included from the same checkout. `web/build.gradle.kts` sets Java/Kotlin target 17, a 4096 MB TeaVM worker heap, strict JavaScript semantics, source maps, and readable non-obfuscated output.
- The tested clean published baseline used gdx-teavm source commit `5e3db3d630d92056f1de14b6f25b8ab84ad48976` and published TeaVM 0.16.0 dependencies.
- `./gradlew :core:compileKotlin --console=plain --no-daemon "-Pkotlin.compiler.execution.strategy=in-process"` — **passed**.
- `SaveLocationTest` passed all three cases, including virtual file listing and all eight combinations of local/external save existence and storage preference.

## TeaVM 0.16.0 current diagnostics

- Published baseline command: `./gradlew -Pweb "-PgdxTeaVMSource=C:/gits/gdx-teavm-master-review" :web:gdx_teavm_web_js_build --console=plain --no-daemon "-Pkotlin.compiler.execution.strategy=in-process"`. This uses gdx-teavm source commit `5e3db3d630d92056f1de14b6f25b8ab84ad48976`, published TeaVM 0.16.0 modules, and no TeaVM source substitution. Dependency report verified all TeaVM coordinates at 0.16.0. JS generation failed with 48 unique missing symbols: 32 classes and 16 methods. Log: `web/build/diagnostics/teavm-js-0.16-published-baseline.log`.
- Upstream 0.16.0 already supplies `Field.getFloat`, removing that one method diagnostic from the previous 0.15 baseline. The backend still lacks `Pixmap(ByteBuffer)` upstream.
- Source-fork command: `./gradlew -Pweb "-PgdxTeaVMSource=C:/gits/gdx-teavm-unciv" -PincludeTeaVMSource=true "-PteavmPath=C:/gits/teavm-unciv" :web:gdx_teavm_web_js_build --console=plain --no-daemon "-Pkotlin.compiler.execution.strategy=in-process"`. gdx-teavm source commit `19c58e88fd0b9fd8c7f8d710efbfa31bff4a9617` contains the tested Pixmap constructor fix. TeaVM source commit `5c6b80cb6f9b8884cea99cfa9b063131992f10ab` is based on TeaVM 0.16.0 tag `e87d452e4bd70c77b9e19314a46c3d6971c148b6`. Gradle resolved `teavm-classlib:0.16.0` to project `:teavm-unciv:classlib`, and the compiler, core, and JSO modules to corresponding source projects; the backend resolved to `:gdx-teavm-unciv:backends:backend-web`.
- The TeaVM source build produced `classlib-0.16.0-SNAPSHOT-original.jar` with SHA-256 `C7BC88416631E3BEA9E1DC22205DB3DC1DBD84772FB6669943A53FF99300CD88`; its relocated compiler jar SHA-256 was `4C122373F1714010BA9D96A845B04F395B227C63788E986DE93B891C1D17D6E6`. This is a Gradle project substitution, not task-level file replacement. The backend plugin derives both the target compiler classpath and `gdx.teavm.classpath` property from the same resolved target classpath.
- Combined JS generation failed with 42 unique missing symbols: 29 classes and 13 methods. Compared with the published 0.16 baseline it removed `AtomicReferenceArray`, `LockSupport`, `ThreadFactory`, `RejectedExecutionException`, `Pixmap(ByteBuffer)`, `Charset.isSupported`, and `UUID(long,long)`. It exposed one additional reachable symbol, `ScheduledThreadPoolExecutor`. Full log: `web/build/diagnostics/teavm-js-0.16-fork-combined.log`.
- Both 0.16 runs are compiler diagnostics only. Neither generated a validated browser game.

## Historical TeaVM 0.15.0 diagnostics

The following 0.15.0 reports are preserved to document earlier work. They are not the current compatibility baseline and their class-library fixes are not recommended for a 0.16 build.

The published-dependency run reported 49 unique missing symbols: 32 class diagnostics and 17 method diagnostics. This count is after de-duplication; the raw log has many repeated call chains. Four class gaps and one method gap were resolved by the local overlays, while the executor path introduced the new scheduled-executor gap; the overlaid result has 45 unique diagnostics.

## Remaining overlaid diagnostics by likely owner

| Owner / call-chain group | Missing classes | Missing methods | Evidence and next owner |
| --- | --- | --- | --- |
| TeaVM class library: Java concurrency used by Kotlin coroutines and SLF4J | `java.util.concurrent.LinkedBlockingQueue`, `java.util.concurrent.locks.ReentrantReadWriteLock`, `ReentrantReadWriteLock.ReadLock`, `ReentrantReadWriteLock.WriteLock`, `java.util.concurrent.ScheduledThreadPoolExecutor` | — | `LinkedBlockingQueue` is reached from SLF4J `SubstituteLoggerFactory`; lock classes through coroutine stack recovery; the scheduled executor is reached from `kotlinx.coroutines.internal.ConcurrentKt` during `Concurrency.stopThreadPools`. Keep real queue, lock, scheduling, and cancellation semantics; do not replace them with no-op stand-ins. |
| TeaVM class library: thread APIs | — | `java.lang.Thread.enumerate(Thread[])`, `java.lang.Thread.setContextClassLoader(ClassLoader)`, `kotlinx.coroutines.scheduling.CoroutineScheduler$Worker.setContextClassLoader(ClassLoader)` | `Thread.enumerate` is called by `UncivGame.logRunningThreads()` during `dispose`; context-loader calls are from coroutine worker setup. The UI lifecycle is single-threaded in the browser, but thread-pool behavior still needs a correct browser implementation or a deliberate runtime boundary. `ThreadFactory` now resolves through the local overlay. |
| TeaVM class library: networking, TLS, selectors, and crypto pulled by CIO/Ktor | `java.net.ConnectException`, `InetAddress`, `InetSocketAddress`, `Proxy`, `Proxy.Type`, `ProxySelector`, `SocketAddress`, `SocketTimeoutException`, `java.nio.channels.Selector`, `SelectorProvider`, `java.security.cert.CertificateFactory`, `X509Certificate`, `MessageDigest`, `Signature`, `ECPoint`, `javax.crypto.Cipher`, `javax.net.ssl.SSLException`, `TrustManagerFactory`, `javax.security.auth.x500.X500Principal` | `java.security.SecureRandom.getInstanceStrong()` | Ktor/CIO traces include `ConnectTimeoutException extends ConnectException` and `TLSException extends SSLException`; these symbols are linked even though browser networking should use browser APIs. Avoid implementing JVM sockets for the browser. See the HTTP engine recommendation below. |
| TeaVM class library: file and class/resource APIs pulled by libraries | `java.nio.file.attribute.DosFileAttributes` | `java.io.File.toPath()`, `ClassLoader.getResources(String)`, `ClassLoader.getSystemResources(String)`, `ClassLoader.loadClass(String)`, `Package.getSpecificationVersion()` | `File.toPath` appears under mod-font scanning; class/resource lookup appears in SLF4J and Ktor logger setup. Check whether the browser needs each path before extending TeaVM classlib. |
| TeaVM class library: other reachable JDK methods | `java.lang.management.ManagementFactory`, `java.text.Collator` | `java.lang.reflect.Field.getFloat(Object)`, `java.lang.Runtime.maxMemory()`, `java.nio.charset.Charset.isSupported(String)`, `java.util.Base64.Decoder.wrap(InputStream)`, `java.util.Base64.Encoder.wrap(OutputStream)`, `java.util.Locale.forLanguageTag(String)`, `java.util.UUID.<init>(long,long)` | Callers are spread across game diagnostics, locale selection, and reachable libraries. `ManagementFactory` is specifically reached by `UncivGame.getGcCount()`; `Locale.forLanguageTag` is reached through `LocaleCode.locale()` while constructing the game-start screen. Preserve normal JVM behavior while adding browser support. |
| gdx-teavm web backend emulation | — | — | The published backend lacks `Pixmap(ByteBuffer)` at `Github.getPreviewImageOrNull()` (`core/src/com/unciv/logic/github/Github.kt:152`). The local backend commit adds the overload forwarding the buffer's position and remaining range; this diagnostic is resolved in the combined overlay compile. |

## HTTP engine boundary recommendation

`core/src/com/unciv/logic/UncivKtor.kt` constructs a global `HttpClient(CIO)`. Other clients also select CIO directly in `ChatWebSocket`, `ApiVersion.detect`, and `ApiV2Wrapper`. The core version catalog's `ktor-client` bundle exposes `ktor-client-cio` along with the common client, WebSockets, encoding, and content negotiation modules. This places JVM socket, selector, TLS, and crypto references in the browser's reachable dependency graph before any game network action.

Recommended small platform seam: centralize client creation/engine selection behind Unciv's existing HTTP client boundary, route all four client construction sites through it, and let the web runtime provide a browser Fetch/WebSocket-backed Ktor engine. Keep the current CIO engine for desktop and Android. A shared mutable engine factory with CIO as its default is not sufficient by itself: it leaves CIO reachable from the core bytecode and Ktor dependencies on TeaVM's target classpath. The web integration must select its browser engine before any client is created and ensure the CIO implementation is excluded from the web compiler graph while preserving the common Ktor client APIs and all four call-site configurations. Do not replace requests with empty responses, shadow the shared client with a null/throwing implementation, or disable network behavior. If a supported TeaVM-compatible Ktor browser engine is unavailable, implement and validate a separate browser adapter; Kotlin/JS's Ktor JS engine should not be assumed to run under TeaVM.

The HTTP engine boundary affects even offline startup: `UncivGame.create()` loads settings, whose migration reaches `ChatWebSocket` and constructs its CIO client. Making that client lazy could defer construction, but would not by itself remove the reachable CIO/JVM networking classes from TeaVM analysis or prove the menu launches. Before claiming a runnable vanilla menu, either wire a real browser engine at startup or establish a compiler/runtime boundary that removes the CIO engine from the web target while preserving the existing clients for desktop and Android. Any staged exclusion of network features must be explicit and preserve failure/null behavior.

## Suggested review-sized stages

1. **Build wiring** — opt-in module, Java 17, pinned published plugin/runtime, and JS task. This stage is configured; compiler validation currently stops at TeaVM analysis.
2. **Reusable TeaVM class-library fixes** — work from the exact listed dependencies and stack traces, starting with coroutines' atomic/queue/lock APIs. Keep behavior equivalent; no fake return values or no-op locks.
3. **Browser backend fixes** — resolve the `Pixmap(ByteBuffer)` API mismatch in gdx-teavm and verify font/image/resource handling.
4. **Game lifecycle and local persistence** — validate start screen, save listing, create/load, autosave, and reload in the browser. `SaveLocationTest` covers virtual local handles on the JVM only; it does not prove browser persistence.
5. **Networking** — introduce the shared client-engine seam and an actual browser transport before claiming Github, mod downloads, multiplayer HTTP, or WebSockets work.
6. **Browser runtime proof** — launch the generated site and observe menu, game creation, map, unit move, end turn, and save/reload. A successful TeaVM compile does not prove any of these.

At this point, Kotlin compilation and the project save-location tests pass. TeaVM JS compilation still fails with the remaining diagnostics; browser launch and all playable-game checks remain unverified.
