#!/usr/bin/env node

const { execFileSync } = require('node:child_process');

const MAX_COUNTED_FILES = 100;

function parseArgs(argv) {
  const args = {};
  for (let index = 0; index < argv.length; index += 1) {
    const value = argv[index];
    if (!value.startsWith('--')) continue;
    const name = value.slice(2);
    args[name] = argv[index + 1] && !argv[index + 1].startsWith('--')
      ? argv[++index]
      : true;
  }
  return args;
}

function isExcluded(fileName) {
  const normalized = String(fileName).replaceAll('\\', '/');
  const lower = normalized.toLowerCase();
  return lower.startsWith('web/')
    || lower.startsWith('docs/')
    || lower.startsWith('plans/')
    || lower.endsWith('.md');
}

function changedFiles(baseRef, headRef) {
  const output = execFileSync(
    'git',
    ['diff', '--name-only', '--diff-filter=ACDMRT', `${baseRef}..${headRef}`],
    { encoding: 'utf8' },
  );
  return [...new Set(output.split(/\r?\n/).map((file) => file.trim()).filter(Boolean))];
}

function checkStackSize(baseRef, headRef, maxCountedFiles = MAX_COUNTED_FILES) {
  const files = changedFiles(baseRef, headRef);
  const excludedFiles = files.filter(isExcluded);
  const countedFiles = files.filter((file) => !isExcluded(file));
  const result = {
    baseRef,
    headRef,
    maxCountedFiles,
    totalChangedFiles: files.length,
    excludedFiles: excludedFiles.length,
    countedFiles: countedFiles.length,
    countedFileNames: countedFiles,
  };
  process.stdout.write(`${JSON.stringify(result, null, 2)}\n`);
  if (countedFiles.length > maxCountedFiles) {
    process.stderr.write(
      `Stack segment exceeds ${maxCountedFiles} counted files: ${countedFiles.length}. Split the segment before continuing.\n`,
    );
    return false;
  }
  return true;
}

function main() {
  const args = parseArgs(process.argv.slice(2));
  const baseRef = args['base-ref'] || process.env.STACK_BASE_REF;
  const headRef = args['head-ref'] || process.env.STACK_HEAD_REF || 'HEAD';
  if (!baseRef) {
    throw new Error('Usage: check-stack-size.js --base-ref <parent> [--head-ref <head>]');
  }
  if (!checkStackSize(baseRef, headRef)) process.exitCode = 2;
}

if (require.main === module) main();

module.exports = { MAX_COUNTED_FILES, isExcluded, checkStackSize };
