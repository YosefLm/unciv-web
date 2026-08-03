const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..', '..');

test('web startup has the TeamVM ruleset hydration hook and transient-field fallback', () => {
  const fallback = fs.readFileSync(path.join(root, 'core/src/com/unciv/json/WebJsonFallback.kt'), 'utf8');
  const ruleset = fs.readFileSync(path.join(root, 'core/src/com/unciv/models/ruleset/Ruleset.kt'), 'utf8');
  assert.match(fallback, /fun hydrateRulesetTechs\(/);
  assert.match(fallback, /JsonReader\(\)\.parse\(file\)/);
  assert.match(ruleset, /WebJsonFallback\.hydrateRulesetTechs\(this, techFile\)/);
  assert.match(fallback, /Modifier\.isTransient/);
});
