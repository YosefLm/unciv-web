# P03 — Necessity and deduplication audit

## Method

Review every changed file and hunk against clean upstream. For each change ask:

1. Does clean upstream fail without it?
2. Is it required by a supported web capability?
3. Is the defect actually in TeaVM or gdx-TeaVM?
4. Is equivalent behavior already present?
5. Can one helper preserve the same order, side effects, threading, and JVM behavior?
6. Is there a focused regression and an E2E proof?

## Decisions

- Keep direct `PlatformCapabilities.current` reads when the surrounding action
  has different semantics. A common property read is not duplicate behavior.
- Keep core/web same-name files when they are source-set overrides. Merging them
  would change the platform boundary rather than deduplicate it.
- Keep one `WebJsonFallback` owner for TeamVM JSON repairs. Do not copy hydration
  logic into loaders or browser runners.
- Move a workaround to a `YosefLm/*` runtime fork when an A/B build proves the
  runtime fix preserves JVM/Desktop/Android behavior and removes Unciv code.
- Delete obsolete CI, generated-source, or compatibility workarounds only after
  a negative test proves the supported TeamVM snapshot no longer needs them.

## Evidence required

Record each decision in `tasks.csv` with the affected files, failure, ownership,
test, and final action (`KEEP`, `CONSOLIDATE`, `MOVE_TO_RUNTIME_FORK`, `DELETE`,
or `REPLACE`).

## Completed audit (2026-08-04)

The final audit is recorded by change ID in `tasks.csv`. It keeps the narrow
web-only `Log`, filesystem, keyboard, map-resource, JSON/reflection, and
ThreadLocalRandom boundaries; consolidates exact duplicate registration and
runner logic; and records no runtime fork because the latest snapshots pass
without a proven fork A/B benefit. The duplicate `PromotionScreenColors`
registration was removed. The provider descriptor was restored because the
existing CI guard and the provider class require it. Negative evidence is in
`tmp/p03-negative-random-browser.log` and `tmp/p03-negative-json-browser.log`.
