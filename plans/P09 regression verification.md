# P09 — Regression verification

For the baseline and every stack branch, run the exact upstream command:

```text
./gradlew classes check --no-build-cache cleanTest test tests:test
```

Compare test counts and failures with the clean pinned checkout. Run
`git diff --check`, conflict-marker scans, ancestry checks, and the counted-file
limit check for each immediate parent/head pair. A missing, blocked,
informational, or stale result is not a pass.
