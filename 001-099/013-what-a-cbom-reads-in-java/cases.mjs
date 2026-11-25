#!/usr/bin/env node
// Lists, for each case of this lab, what a CBOM says about its lines.
//
//   node cases.mjs <cbom.json> <source folder>
//
// A case starts at a comment "// [ID] text" (probe) or "// N. text" (app) and
// ends at the next one. An entry belongs to a case when the CBOM gives it an
// occurrence, file and line, inside that range.
import { readFileSync, readdirSync } from 'node:fs';
import { basename, join } from 'node:path';

const [cbomPath, sourceRoot] = process.argv.slice(2);
if (!cbomPath || !sourceRoot) {
  console.error('usage: node cases.mjs <cbom.json> <source folder>');
  process.exit(2);
}

function javaFiles(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap((entry) => {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) return entry.name === 'target' ? [] : javaFiles(path);
    return entry.name.endsWith('.java') ? [path] : [];
  });
}

const MARK = /^\s*\/\/ (?:\[([A-Z]\d+)\]|(\d)\.) (.+)$/;

function readCases(root) {
  const cases = [];
  for (const file of javaFiles(root).sort()) {
    const lines = readFileSync(file, 'utf8').split('\n');
    let current = null;
    lines.forEach((line, index) => {
      const match = MARK.exec(line);
      if (!match) return;
      if (current) current.end = index;
      current = { id: match[1] ?? match[2], text: match[3], file: basename(file), start: index + 1, end: lines.length, entries: [] };
      cases.push(current);
    });
  }
  return cases;
}

/** What the entry says, in one line: its name, then every property the CBOM gives it. */
function describe(component) {
  const crypto = component.cryptoProperties ?? {};
  const algorithm = crypto.algorithmProperties;
  const material = crypto.relatedCryptoMaterialProperties;
  const protocol = crypto.protocolProperties;
  if (algorithm) {
    const parts = [algorithm.primitive, algorithm.parameterSetIdentifier, algorithm.mode, algorithm.padding, algorithm.curve]
      .filter(Boolean);
    const functions = (algorithm.cryptoFunctions ?? []).join('+');
    return `algorithm  ${component.name}  [${parts.join(', ')}${functions ? `; ${functions}` : ''}]`;
  }
  if (material) return `material   ${material.type}${material.size ? ` ${material.size} bits` : ', no size'}`;
  if (protocol) return `protocol   ${component.name}  [${[protocol.type, protocol.version].filter(Boolean).join(' ')}]`;
  return `${crypto.assetType ?? 'asset'}  ${component.name}`;
}

/** Occurrences as { file, line, call }, from either CBOM dialect this lab meets. */
function occurrences(component) {
  const found = (component.evidence?.occurrences ?? []).map((occurrence) => ({
    file: basename(occurrence.location ?? ''),
    line: occurrence.line,
    call: /([\w$]+)#([\w<>]+)/.exec(occurrence.additionalContext ?? '')?.slice(1).join('#') ?? '',
  }));
  for (const property of component.properties ?? []) {
    if (property.name !== 'cdx:crypto:sourceLocation') continue;
    const [file, line] = property.value.split(':');
    found.push({ file: basename(file), line: Number(line), call: '' });
  }
  return found;
}

const cbom = JSON.parse(readFileSync(cbomPath, 'utf8'));
const assets = (cbom.components ?? []).filter((component) => component.type === 'cryptographic-asset');
const cases = readCases(sourceRoot);
const outside = [];
const unlocated = [];

for (const component of assets) {
  const places = occurrences(component);
  if (places.length === 0) {
    unlocated.push(component.name);
    continue;
  }
  for (const place of places) {
    const owner = cases.find((c) => c.file === place.file && place.line >= c.start && place.line <= c.end);
    const entry = { line: place.line, text: `${describe(component)}  <- ${place.call || 'no call recorded'}` };
    if (owner) owner.entries.push(entry);
    else outside.push(`${place.file}:${place.line}  ${entry.text}`);
  }
}

const named = (c) => c.entries.some((entry) => /^(algorithm|protocol)/.test(entry.text));
for (const c of cases) {
  console.log(`${c.id}  ${c.text}  (${c.file}:${c.start}-${c.end})`);
  if (c.entries.length === 0) console.log('       nothing');
  for (const entry of c.entries.sort((a, b) => a.line - b.line)) {
    console.log(`       line ${String(entry.line).padStart(3)}  ${entry.text}`);
  }
}

console.log('');
console.log(`${assets.length} cryptographic assets in ${basename(cbomPath)}`);
console.log(`${cases.length} cases: ${cases.filter(named).length} with an algorithm or a protocol, ` +
  `${cases.filter((c) => c.entries.length > 0 && !named(c)).length} with key material only, ` +
  `${cases.filter((c) => c.entries.length === 0).length} with nothing`);
if (outside.length) console.log(`Occurrences outside every case:\n  ${outside.join('\n  ')}`);
if (unlocated.length) console.log(`Assets with no location (${unlocated.length}): ${unlocated.join(', ')}`);
