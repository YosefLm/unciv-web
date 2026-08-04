# P05 — TeamVM JSON compatibility

Keep TeamVM-only repairs behind the shared `WebJsonFallback` boundary.

Required regression cases:

- `TechColumn[]` loading retains nested `Technology` objects and reveal-tech
  lookup during start-new-game.
- Missing game parameters, civilizations, base ruleset, and tiles hydrate from
  raw JSON without changing JVM behavior.
- City manager backreferences do not recurse during web serialization.
- Clean JVM serialization and save/load tests remain unchanged.

Run a negative test with each repair removed, then rerun the full inherited JVM
and browser gates before deciding whether a runtime-fork change can replace it.
