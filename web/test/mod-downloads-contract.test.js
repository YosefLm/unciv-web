import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const read = (...parts) => fs.readFileSync(path.join(root, ...parts), "utf8");

test("browser mod downloads retain ZIP transport, HTTP fetch, and fixture coverage", () => {
  const zipInterop = read("web", "src", "main", "java", "com", "unciv", "logic", "github", "WebZipInterop.java");
  const fetch = read("web", "src", "main", "java", "com", "unciv", "logic", "web", "WebFetch.java");
  const api = read("web", "src", "main", "kotlin", "com", "unciv", "logic", "github", "GithubAPI.kt");
  const fixture = path.join(root, "web", "src", "main", "resources", "webtest", "mods", "test-mod.zip");

  assert.match(zipInterop, /JSZip|unzip|zip/i);
  assert.match(fetch, /fetch|XMLHttpRequest/);
  assert.match(api, /download|Github/);
  assert.equal(fs.existsSync(fixture), true);
  assert.ok(fs.statSync(fixture).size > 0);
});
