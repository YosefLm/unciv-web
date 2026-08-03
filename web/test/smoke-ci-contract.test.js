import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();

test("smoke CI builds the web bundle before browser checks", () => {
  const workflow = fs.readFileSync(path.join(root, ".github", "workflows", "web-build.yml"), "utf8");
  const runner = fs.readFileSync(path.join(root, "scripts", "web", "run-web-validation.js"), "utf8");

  assert.match(workflow, /webBuildJs/);
  assert.match(workflow, /playwright|browser/i);
  assert.match(runner, /WEB_BASE_URL|validation/i);
});
