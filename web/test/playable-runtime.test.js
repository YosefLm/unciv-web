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
  assert.match(validation, /clickNextTurnAndWait/);
  assert.match(validation, /retrying typed nextTurn/);
  assert.match(validation, /visible Skip action's/);
  assert.match(validation, /unit\.hasMovement\(\)/);
  assert.match(validation, /due-unit recovery was not observable/);
  assert.match(validation, /if \(screen\.isPlayersTurn\)/);
  assert.doesNotMatch(validation, /maxAttemptsPerTurn = 120/);
  assert.match(validation, /if \(!screen\.isPlayersTurn\)/);
  assert.match(validation, /nextTurnButton\.update\(\)/);
  assert.match(validation, /previousCheckForDueUnits/);
  assert.match(validation, /filterIsInstance<Popup>/);
  assert.match(validation, /val searchRoots = listOfNotNull\(popup, root\)/);
});
