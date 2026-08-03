import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");

test("clipboard abstraction keeps native fallback and browser bridge", () => {
  const clipboard = read("core", "src", "com", "unciv", "utils", "AppClipboard.kt");
  const bridge = read("web", "src", "main", "java", "com", "unciv", "app", "web", "WebClipboardBridge.java");

  assert.match(clipboard, /Gdx\.app\.clipboard/);
  assert.match(clipboard, /invokeWebRead/);
  assert.match(clipboard, /invokeWebWrite/);
  assert.match(bridge, /readTextAsync/);
  assert.match(bridge, /writeTextAsync/);
});
