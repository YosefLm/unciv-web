import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const shim = fs.readFileSync(
  path.join(root, 'src/main/java/org/teavm/classlib/java/util/concurrent/TThreadLocalRandom.java'),
  'utf8',
);

test('web TeaVM ThreadLocalRandom overlay keeps static initialization seed-safe', () => {
  assert.match(shim, /class TThreadLocalRandom extends TRandom/);
  assert.match(shim, /public void setSeed\(long seed\)/);
  assert.doesNotMatch(shim, /throw\s+new\s+UnsupportedOperationException/);
  assert.match(shim, /Math\.random\(\)/);
});
