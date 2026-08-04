# P09 — Regression verification

For the baseline and every stack branch, run the exact upstream command:

```text
./gradlew classes check --no-build-cache cleanTest test tests:test
```

Compare test counts and failures with the clean pinned checkout. Run
`git diff --check`, conflict-marker scans, ancestry checks, and the counted-file
limit check for each immediate parent/head pair. A missing, blocked,
informational, or stale result is not a pass.

## Completed E2E evidence (2026-08-04)

The clean baseline passed with 69 actionable tasks; P1 passed with 74; P2
passed with 74; and the final P3 source passed with 75. The final P3 rerun is
`tmp/p03-final-exact-suite-final-source.log`. Each branch is an immediate
descendant of its parent, remains under the 100 counted-file limit, and has
passing `git diff --check` and conflict-marker scans.
