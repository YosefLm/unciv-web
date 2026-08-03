import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");

test("deep validation layer keeps browser tests, WAR preloads, and Pages CI wired", () => {
  const gradle = read("web", "build.gradle.kts");
  const workflow = read(".github", "workflows", "web-build.yml");
  const runner = read("scripts", "web", "run-js-browser-tests.js");
  const clickOps = read("scripts", "web", "lib", "clickops-common.js");
  const launcher = read("web", "src", "main", "java", "com", "unciv", "app", "web", "WebLauncher.java");
  const preload = read("web", "src", "main", "java", "com", "unciv", "app", "web", "WebAssetPreloadApplicationListener.java");
  const mainMenu = read("core", "src", "com", "unciv", "ui", "screens", "mainmenuscreen", "MainMenuScreen.kt");

  assert.match(gradle, /generateWebJsTestSuite/);
  assert.match(gradle, /webGenerateWarPreloads/);
  assert.match(gradle, /webVerifyWarPreloads/);
  assert.match(gradle, /webBuildJs/);
  assert.match(workflow, /webBuildJs/);
  assert.match(workflow, /pages|github-pages|deploy/i);
  assert.match(runner, /chromium|playwright/i);
  assert.match(clickOps, /screenshot|target/i);
  assert.match(workflow, /preload|war/i);
  assert.match(launcher, /WebAssetPreloadApplicationListener/);
  assert.match(preload, /WebPreloadApplicationListener/);
  assert.match(preload, /ExtraImages\/banner\.png/);
  assert.match(mainMenu, /enableEasterEggs\s*&&\s*Gdx\.app\.type\s*!=\s*Application\.ApplicationType\.WebGL/);
});
