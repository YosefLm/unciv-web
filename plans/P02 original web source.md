# P02 — Original web source

## Objective

Reproduce the minimum source needed for the web implementation represented by
PR #14709, then apply only deterministic fixes proven necessary against the
latest TeamVM snapshot.

## Included behavior

- The isolated `web` Gradle module and JS/WASM entry points.
- Core platform capability boundaries and web source-set implementations.
- TeamVM asset loading, logging, font, locale, file, and runtime bridges.
- The ruleset hydration repair for missing nested `Technology` objects.
- The web-only JSON back-reference repair for city managers.
- The web build and focused bootstrap contracts.

## Necessity audit

Every non-web core edit must have a demonstrated clean-upstream failure or be
the smallest platform boundary required to preserve existing JVM behavior.
Repeated capability reads are not automatically deduplicated: they remain
separate when their side effects, evaluation order, or user-facing behavior
differ. Source-set overrides remain intentional and are not merged merely
because their filenames match.

## Acceptance

- Maximum: 100 total changed files, 66 counted files for this boundary.
- Independent JVM suite passes.
- `:web:webBuildJs` passes with the latest TeamVM snapshot.
- Bootstrap, startup, and start-new-game focused contracts pass.
