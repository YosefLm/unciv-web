const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');

test('web visibility compatibility guards the TeamVM null pause race', () => {
  const launcher = fs.readFileSync(path.resolve(__dirname, '../src/main/java/com/unciv/app/web/WebLauncher.java'), 'utf8');
  const compat = fs.readFileSync(path.resolve(__dirname, '../src/main/java/com/unciv/app/web/WebVisibilityCompat.java'), 'utf8');
  assert.match(launcher, /WebVisibilityCompat\.install\(\)/);
  assert.match(compat, /visibilitychange/);
  assert.match(compat, /Cannot read properties of null/);
  assert.match(compat, /\$pause/);
});
