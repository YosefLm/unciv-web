# P01 — Upstream baseline

## Objective

Create the only regression baseline from `yairm210/Unciv` master at
`f2e3017b77ad9c7dda04549b1e8e7ddaddbdccf5`.

## Required checks

- Verify the branch is an exact descendant of the pinned upstream commit.
- Run `./gradlew classes check --no-build-cache cleanTest test tests:test`.
- Save the exact JDK, Gradle, Kotlin, Node, Playwright, and TeamVM/gdx-TeamVM
  versions in the local evidence directory.
- Treat every baseline failure as a blocker; do not convert it to an
  informational result.

## Necessity decision

The pinned upstream checkout is required because the previous stack base was
obsolete. No application change belongs in this step.
## Completed E2E evidence (2026-08-04)

The pinned clean checkout passed the exact suite in
`tmp/upstream-baseline-exact.log` with 69 actionable tasks. The recorded
toolchain is Java 22.0.2, Gradle 8.11.1, Kotlin 2.3.0, Node 26.4.0, npm
11.17.0, Playwright 1.62.1, and the latest recorded TeamVM snapshots. This
baseline remains the only upstream-regression comparison for every descendant.
