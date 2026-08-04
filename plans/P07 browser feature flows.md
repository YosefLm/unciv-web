# P07 — Browser feature flows

Add web tests only where the branch contains web classes or web implementation
changes. Every new feature has one focused contract and one real-browser flow;
descendants execute all inherited tests.

Required cumulative flows are capability gating, new game/world entry,
gameplay turns, file chooser/save-load, mod download, audio, multiplayer
transport and chat, WAR preload, and deep validation. A flow passes only with
zero crash screens, unsupported-operation failures, page errors, console
errors, and JavaScript test failures.

## Completed E2E evidence (2026-08-04)

The final source passes phase-4 validation 12/12, the browser JS suite 240/240
with 18 explicit ignored tests, file I/O with 19 blob events, strict clickops
multiplayer with host/guest chat and turn synchronization, audio/mod flows,
all three WAR roles, and the local Pages smoke. Final reports contain zero
page errors, console errors, request failures, crash screens, and unsupported
operation failures.
