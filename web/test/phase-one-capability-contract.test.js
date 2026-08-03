import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const source = fs.readFileSync(
  path.join(
    process.cwd(),
    "core",
    "src",
    "com",
    "unciv",
    "platform",
    "PlatformCapabilities.kt",
  ),
  "utf8",
);

test("phase-one capability profile disables every unsupported web service", () => {
  const phaseOne = source.match(/fun webPhase1\(\)(?:\s*:\s*Features)?\s*=\s*Features\(([^)]*)\)/s)?.[1] ?? "";

  assert.match(phaseOne, /onlineMultiplayer\s*=\s*false/);
  assert.match(phaseOne, /customFileChooser\s*=\s*false/);
  assert.match(phaseOne, /onlineModDownloads\s*=\s*false/);
  assert.match(phaseOne, /systemFontEnumeration\s*=\s*false/);
  assert.match(phaseOne, /backgroundThreadPools\s*=\s*false/);
});
