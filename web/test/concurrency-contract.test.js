import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

const root = process.cwd();

test("web concurrency keeps the portable continuation and queue paths", () => {
  const concurrency = fs.readFileSync(
    path.join(root, "web", "src", "main", "kotlin", "com", "unciv", "utils", "Concurrency.kt"),
    "utf8",
  );
  const queue = fs.readFileSync(
    path.join(root, "core", "src", "com", "unciv", "utils", "LongPriorityQueue.kt"),
    "utf8",
  );

  assert.match(concurrency, /Continuation/);
  assert.match(concurrency, /withContext|launch|async/);
  assert.match(queue, /PriorityQueue|LongPriorityQueue/);
});
