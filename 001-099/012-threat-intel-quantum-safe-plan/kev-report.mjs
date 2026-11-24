#!/usr/bin/env node
// How many of the vulnerabilities attackers are known to exploit are failures of cryptography?
//
// The longer version of kev-crypto.mjs, the script the note prints. It reads CISA's Known Exploited
// Vulnerabilities (KEV) catalog and counts the entries whose CWE is one of the 32 that the OWASP Top 10:2025
// maps to A04 Cryptographic Failures (a04-cwes.json), then breaks the count down.
//
//   node kev-report.mjs --file kev-2026-11-23.json   the saved copy the note used
//   node kev-report.mjs                              today's catalog, fetched from cisa.gov
//   node kev-report.mjs --since 2026-11-01           also exits 1 when an entry was added since that day
//
// Node.js 22 or later. No dependencies.

import { readFile } from 'node:fs/promises';

const FEED = 'https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json';

// Not in A04: the OWASP Top 10:2025 files these under A07 Authentication Failures. They are the two groups
// closest to certificates and keys, so the count shows them apart instead of leaving them out of sight.
const APART = [
  { label: 'Certificate validation (CWE-295, 297, 298, 299)', ids: [295, 297, 298, 299] },
  { label: 'Hard-coded credentials (CWE-259, 798)', ids: [259, 798] },
];

function option(name) {
  const at = process.argv.indexOf(name);
  return at === -1 ? undefined : process.argv[at + 1];
}

async function catalog() {
  const file = option('--file');
  if (file) return JSON.parse(await readFile(file, 'utf8'));
  const response = await fetch(FEED);
  if (!response.ok) throw new Error(`cisa.gov answered ${response.status}`);
  return response.json();
}

const cweIds = (entry) => (entry.cwes ?? []).map((cwe) => Number(cwe.replace('CWE-', '')));
const percent = (part, whole) => `${((100 * part) / whole).toFixed(2)}%`;
const column = (text, width) => String(text).padEnd(width);

const mapped = JSON.parse(await readFile(new URL('./a04-cwes.json', import.meta.url), 'utf8'));
const names = new Map(mapped.cwes.map((cwe) => [cwe.id, cwe.name]));
const kev = await catalog();
const entries = kev.vulnerabilities;

const withCwe = entries.filter((entry) => cweIds(entry).length > 0);
const crypto = entries
  .filter((entry) => cweIds(entry).some((id) => names.has(id)))
  .sort((a, b) => a.dateAdded.localeCompare(b.dateAdded) || a.cveID.localeCompare(b.cveID));

console.log(`KEV catalog ${kev.catalogVersion}, released ${kev.dateReleased}`);
console.log(`Entries: ${entries.length}`);
console.log(`With at least one CWE: ${withCwe.length}. With none: ${entries.length - withCwe.length}`);
console.log(`CWEs mapped to ${mapped.category}: ${names.size}`);
console.log();
console.log(
  `Entries with a CWE of A04: ${crypto.length} ` +
    `(${percent(crypto.length, entries.length)} of the catalog, ${percent(crypto.length, withCwe.length)} of the entries with a CWE)`,
);
console.log(`Of those, used in ransomware campaigns: ${crypto.filter((entry) => entry.knownRansomwareCampaignUse === 'Known').length}`);

console.log('\nBy CWE');
const byCwe = new Map();
for (const entry of crypto) {
  for (const id of cweIds(entry).filter((cwe) => names.has(cwe))) byCwe.set(id, (byCwe.get(id) ?? 0) + 1);
}
for (const [id, count] of [...byCwe].sort((a, b) => b[1] - a[1] || a[0] - b[0])) {
  console.log(`  ${column(`CWE-${id}`, 9)}${column(count, 4)}${names.get(id)}`);
}

console.log('\nBy year added');
const byYear = new Map();
for (const entry of crypto) byYear.set(entry.dateAdded.slice(0, 4), (byYear.get(entry.dateAdded.slice(0, 4)) ?? 0) + 1);
for (const [year, count] of [...byYear].sort()) {
  const added = entries.filter((entry) => entry.dateAdded.startsWith(year)).length;
  console.log(`  ${year}  ${column(count, 4)}of ${added} added that year`);
}

console.log('\nThe entries');
for (const entry of crypto) {
  console.log(
    `  ${entry.dateAdded}  ${column(entry.cveID, 17)}${column(entry.cwes.join(','), 9)}` +
      `${column(entry.knownRansomwareCampaignUse === 'Known' ? 'ransomware' : '', 12)}${entry.vendorProject}: ${entry.product}`,
  );
}

console.log('\nNot in A04, shown apart');
for (const group of APART) {
  const found = entries.filter((entry) => cweIds(entry).some((id) => group.ids.includes(id)) && !crypto.includes(entry));
  console.log(`  ${column(found.length, 4)}${group.label}`);
}

const since = option('--since');
if (since) {
  const added = crypto.filter((entry) => entry.dateAdded >= since);
  console.log(`\nAdded since ${since}: ${added.length}`);
  for (const entry of added) console.log(`  ${entry.dateAdded}  ${entry.cveID}  ${entry.vulnerabilityName}`);
  process.exitCode = added.length > 0 ? 1 : 0;
}
