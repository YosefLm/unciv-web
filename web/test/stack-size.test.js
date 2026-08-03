const test = require('node:test');
const assert = require('node:assert/strict');
const { isExcluded } = require('../../scripts/stack/check-stack-size.js');

test('stack size gate excludes web and explanatory files', () => {
  assert.equal(isExcluded('web/src/main/kotlin/App.kt'), true);
  assert.equal(isExcluded('docs/stack.md'), true);
  assert.equal(isExcluded('PLANS/T13/PLAN.MD'), true);
  assert.equal(isExcluded('README.md'), true);
  assert.equal(isExcluded('core/src/App.kt'), false);
  assert.equal(isExcluded('tests/src/AppTest.kt'), false);
});
