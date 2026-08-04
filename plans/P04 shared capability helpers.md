# P04 — Shared capability boundaries

Audit capability gates across core and web. Consolidate only exact duplicate
predicates whose evaluation and side effects are identical. Keep feature-level
branches separate when they control different UI, persistence, networking,
font, or threading behavior.

The shared `PlatformCapabilities.Features` profile remains the single source of
truth. JVM/Desktop/Android keep the default capabilities; web selects the
restricted profile. Every changed call site gets its inherited JVM test and,
when web code exists, the corresponding cumulative browser contract.
