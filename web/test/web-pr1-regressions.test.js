import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");

test("PR1 keeps web-only compatibility at narrow boundaries", () => {
  const files = read("core", "src", "com", "unciv", "logic", "files", "UncivFiles.kt");
  const menu = read("core", "src", "com", "unciv", "ui", "screens", "mainmenuscreen", "MainMenuScreen.kt");
  const resources = read("core", "src", "com", "unciv", "logic", "map", "mapgenerator", "MapResourceSetting.kt");
  const modernResources = read(
    "core", "src", "com", "unciv", "logic", "map", "mapgenerator", "resourceplacement",
    "StrategicBonusResourcePlacementLogic.kt",
  );
  const log = read("core", "src", "com", "unciv", "utils", "Log.kt");
  const builder = read("web", "src", "main", "java", "com", "unciv", "app", "web", "BuildWebCommon.java");

  assert.match(files, /Application\.ApplicationType\.WebGL/);
  assert.match(files, /getLocalFile\(saveFolder\)\.list\(\)\.asSequence\(\)/);
  assert.match(files, /externalFile\(path\) \?: getLocalFile\(path\)/);
  assert.match(menu, /binding: KeyboardBinding\?/);
  assert.match(menu, /enableKeyboardBindings = Gdx\.app\.type != Application\.ApplicationType\.WebGL/);
  assert.match(resources, /webSelectable: Boolean = true/);
  assert.match(resources, /entries\.filter \{ it\.webSelectable \}/);
  assert.match(modernResources, /ruleset\.technologies\[revealedBy\] \?: return@filter false/);
  assert.match(modernResources, /ruleset\.eras\[technology\.era\(\)\] \?: return@filter false/);
  assert.match(log, /stackTrace\.firstOrNull/);
  assert.match(builder, /com\.unciv\.models\.ruleset\.RulesetObject/);
  assert.match(builder, /PromotionScreenColors/);
});
