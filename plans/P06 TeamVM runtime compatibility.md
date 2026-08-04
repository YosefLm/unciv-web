# P06 — TeamVM runtime compatibility

Use the latest available TeamVM/gdx-TeamVM snapshot first. Inspect TeaVM and
gdx-TeaVM ownership for every unsupported runtime feature. A fork under
`YosefLm/*` is preferred when the defect is runtime-owned and the fork can be
built and consumed reproducibly.

The `ThreadLocalRandom` repair must remain web-only. It must not alter upstream
JVM, Desktop, or Android classes. Verify this with a clean JVM diff and a fresh
browser startup from a new origin.

## Completed E2E evidence (2026-08-04)

The final build uses TeaVM 0.15.0 with backend-web
`fa8a556056b23c543ddbd2a21d48ec99303021f1`, backend-shared
`84d2f38e55e1ab17c1b8c5e6814ab437f110075b`, gdx-freetype-web
`aceeb5dca6fdb19adf6df7095152554d12dcb478`, and asset-loader
`bf7c6da3454fdb0466bdbe648caac502e58c2c8d`. Removing the web random overlay
still compiles but the fresh phase-1 browser gate fails with
`UnsupportedOperationException`; the JVM suite remains unchanged. No
`YosefLm` runtime fork was needed or created.
