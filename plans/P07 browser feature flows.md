# P07 — Browser feature flows

Add web tests only where the branch contains web classes or web implementation
changes. Every new feature has one focused contract and one real-browser flow;
descendants execute all inherited tests.

Required cumulative flows are capability gating, new game/world entry,
gameplay turns, file chooser/save-load, mod download, audio, multiplayer
transport and chat, WAR preload, and deep validation. A flow passes only with
zero crash screens, unsupported-operation failures, page errors, console
errors, and JavaScript test failures.
