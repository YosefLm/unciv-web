# TeaVM Fetch engine contract

This fixture compiles the production `FetchHttpClientEngine` and its service descriptor, then runs the generated JavaScript under Node with a mocked browser `fetch` implementation. It covers GET headers/status/text/body streaming, POST body transmission, non-2xx responses, network rejection, request cancellation, response-body cancellation, and Ktor request timeout cancellation.

Pass a matched TeaVM 0.16 classlib and core. The last validated pair was copied from the source build into temporary files:

- classlib `C:/Users/yosef/AppData/Local/Temp/unciv-fetch-classlib-8d865-pair.jar`, SHA-256 `F06D7ACA51BC48149ABFE33F0DE1D882AA6098408F2C0A8F04CEF8041B3E9F69`
- core `C:/Users/yosef/AppData/Local/Temp/unciv-fetch-core-8d865.jar`, SHA-256 `8D86500720FEAFA9275412C0E74678E9DE652974F0ED9351C1DA1C418A20A4CE`

For a fresh run, build the TeaVM fork jars and copy both to stable files so they cannot change during compilation. Then run from the Unciv root; this uses the checked-in Gradle wrapper and defaults to the full cancellation contract:

```powershell
$classlibJar = Join-Path $env:TEMP 'unciv-fetch-classlib.jar'
$coreJar = Join-Path $env:TEMP 'unciv-fetch-core.jar'
Copy-Item 'C:/gits/teavm-unciv/classlib/build/libs/classlib-0.16.0-SNAPSHOT.jar' $classlibJar
Copy-Item 'C:/gits/teavm-unciv/core/build/libs/core-0.16.0-SNAPSHOT.jar' $coreJar
./gradlew.bat `
  -p web/validation/http-fetch-engine runJavaScriptContract `
  "-PclasslibJar=$classlibJar" `
  "-PcoreJar=$coreJar" `
  --console=plain --no-daemon '-Pkotlin.compiler.execution.strategy=in-process'
```

The full run passed with Node and printed `Fetch engine contract passed`. It checks GET/POST UTF-8 bytes, streamed body data split across the encoding of `é`, headers, non-2xx response preservation, network error/reason propagation, request cancellation, response-body cancellation, and Ktor request timeout cancellation. Set `FETCH_SKIP_CANCEL=1` only to skip cancellation checks, or `FETCH_COROUTINE_CANCEL_REPRO=1` to run only the minimal Kotlin timeout/cancellable-suspension reproduction used to isolate TeaVM stack-recovery behavior.
