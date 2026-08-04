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
