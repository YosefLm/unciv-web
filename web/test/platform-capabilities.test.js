import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();

test("web runtime preserves the phase-one capability baseline and profile selection", () => {
  const capabilities = fs.readFileSync(
    path.join(root, "core", "src", "com", "unciv", "platform", "PlatformCapabilities.kt"),
    "utf8",
  );
  const launcher = fs.readFileSync(
    path.join(root, "web", "src", "main", "java", "com", "unciv", "app", "web", "WebLauncher.java"),
    "utf8",
  );

  assert.match(capabilities, /fun webPhase1\(\)/);
  assert.match(capabilities, /onlineMultiplayer = false/);
  assert.match(capabilities, /onlineModDownloads = false/);
  assert.match(launcher, /setCurrent\((?:PlatformCapabilities\.webPhase1\(\)|features)/);
});
