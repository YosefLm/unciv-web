import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");

test("file I/O layer wires browser chooser, saver/loader, and smoke orchestration", () => {
  const chooser = read("web", "src", "main", "kotlin", "com", "unciv", "logic", "files", "WebFileChooser.kt");
  const saverLoader = read("web", "src", "main", "kotlin", "com", "unciv", "logic", "files", "WebPlatformSaverLoader.kt");
  const launcher = read("web", "src", "main", "java", "com", "unciv", "app", "web", "WebLauncher.java");
  const smoke = read("scripts", "web", "run-web-file-io-smoke.js");

  assert.match(chooser, /loadBinary/);
  assert.match(chooser, /loadText/);
  assert.match(saverLoader, /saveText/);
  assert.match(saverLoader, /loadText/);
  assert.match(launcher, /WebPlatformSaverLoader/);
  assert.match(launcher, /WebFileChooser/);
  assert.match(smoke, /file-io/);
  assert.match(smoke, /launchChromium/);
});
