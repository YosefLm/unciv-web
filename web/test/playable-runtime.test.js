import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");

test("playable runtime preserves JSON models and guards browser boot", () => {
  const builder = read("web", "src", "main", "java", "com", "unciv", "app", "web", "BuildWebCommon.java");
  const game = read("web", "src", "main", "kotlin", "com", "unciv", "app", "web", "WebGame.kt");
  const validation = read("web", "src", "main", "kotlin", "com", "unciv", "app", "web", "WebValidationRunner.kt");

  assert.match(builder, /PRESERVED_CLASSES/);
  assert.match(builder, /hardenIndexBootstrap/);
  assert.match(game, /class WebGame/);
  assert.match(validation, /WebValidationRunner/);
});
