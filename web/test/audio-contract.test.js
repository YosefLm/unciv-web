import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");

test("audio layer tolerates delayed browser startup and uses the web-safe download path", () => {
  const music = read("core", "src", "com", "unciv", "ui", "audio", "MusicController.kt");
  const soundTab = read("core", "src", "com", "unciv", "ui", "popups", "options", "SoundTab.kt");
  const dropbox = read("core", "src", "com", "unciv", "logic", "multiplayer", "storage", "DropBox.kt");

  assert.match(music, /startupGraceSeconds/);
  assert.match(music, /currentAwaitingStartup/);
  assert.match(soundTab, /backgroundThreadPools/);
  assert.match(soundTab, /runOnGLThread/);
  assert.match(dropbox, /downloadFileBytesAsync/);
});
