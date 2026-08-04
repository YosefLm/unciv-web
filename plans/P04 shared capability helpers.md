# P04 — Shared capability boundaries

Audit capability gates across core and web. Consolidate only exact duplicate
predicates whose evaluation and side effects are identical. Keep feature-level
branches separate when they control different UI, persistence, networking,
font, or threading behavior.

The shared `PlatformCapabilities.Features` profile remains the single source of
truth. JVM/Desktop/Android keep the default capabilities; web selects the
restricted profile. Every changed call site gets its inherited JVM test and,
when web code exists, the corresponding cumulative browser contract.

## Completed E2E evidence (2026-08-04)

The feature branch has 95 total changed files and 64 counted files. Its exact
JVM suite and 13/13 Node contracts pass, and the inherited phase-1 browser
flow passes with zero page or console errors. The capability profile remains
shared and platform-specific behavior is not hidden behind a duplicated
helper.
