# P06 — TeamVM runtime compatibility

Use the latest available TeamVM/gdx-TeamVM snapshot first. Inspect TeaVM and
gdx-TeaVM ownership for every unsupported runtime feature. A fork under
`YosefLm/*` is preferred when the defect is runtime-owned and the fork can be
built and consumed reproducibly.

The `ThreadLocalRandom` repair must remain web-only. It must not alter upstream
JVM, Desktop, or Android classes. Verify this with a clean JVM diff and a fresh
browser startup from a new origin.

