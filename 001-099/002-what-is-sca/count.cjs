// Counts what npm audit found, package by package, as the table in the note does.
// The plain report of npm audit does not print the advisories of a package it
// lists under another one (here express and serve-static). The JSON report has all.
//
// Run it in a folder that has package.json and package-lock.json:
//   npm audit --json | node count.cjs
const fs = require('node:fs');

const audit = JSON.parse(fs.readFileSync(0, 'utf8'));
if (!audit.vulnerabilities || !audit.metadata) {
  console.error('npm audit gave no report: ' + JSON.stringify(audit.error || audit));
  process.exit(1);
}
const lock = JSON.parse(fs.readFileSync('package-lock.json', 'utf8'));
const declared = Object.keys(JSON.parse(fs.readFileSync('package.json', 'utf8')).dependencies || {});
const order = ['info', 'low', 'moderate', 'high', 'critical'];
const worstOf = (list) => list.map((a) => a.severity).sort((a, b) => order.indexOf(b) - order.indexOf(a))[0] || '-';

const rows = Object.values(audit.vulnerabilities).map((v) => {
  // Each entry of "via" is an advisory (an object) or the name of another vulnerable package.
  const advisories = v.via.filter((entry) => typeof entry === 'object');
  return {
    name: v.name,
    version: v.nodes.map((node) => lock.packages[node].version).join(', '),
    declared: declared.includes(v.name) ? 'yes' : 'no',
    advisories,
    worst: worstOf(advisories),
    rating: v.severity,
  };
});
rows.sort((a, b) => b.declared.localeCompare(a.declared) || b.advisories.length - a.advisories.length || a.name.localeCompare(b.name));

const counts = audit.metadata.vulnerabilities;
const total = rows.reduce((sum, row) => sum + row.advisories.length, 0);
console.log('packages in package-lock.json: ' + Object.keys(lock.packages).filter((key) => key !== '').length);
console.log(`vulnerable packages: ${counts.total} (critical ${counts.critical}, high ${counts.high}, moderate ${counts.moderate}, low ${counts.low})`);
console.log('advisories: ' + total);

if (rows.length > 0) {
  const line = (cells) => cells.map((cell, i) => String(cell).padEnd([16, 9, 10, 12, 11, 0][i])).join('').trimEnd();
  console.log('');
  console.log(line(['package', 'version', 'declared', 'advisories', 'worst own', 'npm rating']));
  for (const row of rows) console.log(line([row.name, row.version, row.declared, row.advisories.length, row.worst, row.rating]));
  console.log('');
  for (const row of rows) {
    for (const advisory of row.advisories) console.log(`${row.name.padEnd(16)}${advisory.severity.padEnd(10)}${advisory.url}`);
  }
}
