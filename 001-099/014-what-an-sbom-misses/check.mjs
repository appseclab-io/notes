// Reads the CycloneDX SBOMs of this folder and says, for each component we planted in app/, whether each SBOM
// lists it. Node.js 22, no dependencies.
//
//   node check.mjs            the table of the note
//   node check.mjs --detail   every matching entry, with its version, purl and where the tool found it

import fs from 'node:fs';

// What we planted, in the order of the note. `names` are the component names that count as a match.
const PLANTED = [
  { id: 1, what: 'Declared dependency (debug 4.3.1)', names: ['debug'], version: '4.3.1' },
  { id: 2, what: 'Dependency of a dependency (ms 2.1.2)', names: ['ms'], version: '2.1.2' },
  { id: 3, what: 'Development dependency (mime 2.5.2)', names: ['mime'], version: '2.5.2' },
  { id: 4, what: 'Dependency from a URL (is-number 7.0.0)', names: ['is-number'], version: '7.0.0' },
  { id: 5, what: 'Library copied to vendor/ (lodash 4.17.20)', names: ['lodash', 'lodash.js'], version: '4.17.20' },
  { id: 6, what: 'Minified library in public/js/ (jQuery 3.4.1)', names: ['jquery'], version: '3.4.1' },
  { id: 7, what: 'Library loaded from a CDN (axios 0.21.1)', names: ['axios'], version: '0.21.1' },
  { id: 8, what: 'Copied function (escape-html 1.0.3)', names: ['escape-html'], version: '1.0.3' },
  { id: 9, what: 'System package (curl, apk add)', names: ['curl'] },
  { id: 10, what: 'Binary downloaded with curl (jq 1.7.1)', names: ['jq'], version: '1.7.1' },
  { id: 'runtime', what: 'The runtime (Node.js 22.23.4)', names: ['node', 'nodejs', 'node.js'], version: '22.23.4' },
];

// The SBOMs, in the order of the columns.
const SBOMS = [
  { file: 'sbom-npm.json', label: 'npm sbom', target: 'folder' },
  { file: 'sbom-cdxgen.json', label: 'cdxgen', target: 'folder' },
  { file: 'sbom-syft-dir.json', label: 'Syft', target: 'folder' },
  { file: 'sbom-trivy-dir.json', label: 'Trivy', target: 'folder' },
  { file: 'sbom-syft-image.json', label: 'Syft', target: 'image' },
  { file: 'sbom-trivy-image.json', label: 'Trivy', target: 'image' },
];

// Components can nest (CycloneDX allows components inside components): walk them all.
function flatten(components = []) {
  return components.flatMap((component) => [component, ...flatten(component.components)]);
}

// Where the tool says it found the component, when it says.
function where(component) {
  const properties = component.properties ?? [];
  const pick = (test) => properties.filter((p) => test(p.name)).map((p) => p.value);
  const paths = [
    ...pick((name) => /^syft:location:\d+:path$/.test(name)),
    ...pick((name) => name === 'aquasecurity:trivy:FilePath' || name === 'aquasecurity:trivy:PkgPath'),
    ...pick((name) => name === 'SrcFile'),
    ...(component.evidence?.identity ?? []).flatMap((identity) => (identity.methods ?? []).map((m) => m.value)),
    ...(component.evidence?.occurrences ?? []).map((occurrence) => occurrence.location),
  ];
  return [...new Set(paths.filter(Boolean))];
}

// Does the entry say the package is only needed for development?
function isDev(component) {
  if (component.scope === 'optional' || component.scope === 'excluded') return true;
  return (component.properties ?? []).some(
    (p) => /development|:dev$/i.test(p.name) && String(p.value) === 'true',
  );
}

function matches(planted, components) {
  return components.filter((component) => planted.names.includes(String(component.name).toLowerCase()));
}

const detail = process.argv.includes('--detail');
const loaded = SBOMS.map((sbom) => {
  const bom = JSON.parse(fs.readFileSync(new URL(sbom.file, import.meta.url), 'utf8'));
  return { ...sbom, components: flatten(bom.components) };
});

for (const sbom of loaded) {
  const byType = {};
  for (const component of sbom.components) byType[component.type] = (byType[component.type] ?? 0) + 1;
  const types = Object.entries(byType).map(([type, count]) => `${count} ${type}`).join(', ');
  console.log(`${sbom.file}: ${sbom.components.length} components (${types})`);
}
console.log('');

const cell = (planted, sbom) => {
  const found = matches(planted, sbom.components);
  if (found.length === 0) return 'no';
  const exact = planted.version ? found.filter((component) => component.version === planted.version) : found;
  if (exact.length === 0) return `other version (${[...new Set(found.map((c) => c.version))].join(', ')})`;
  const dev = planted.id === 3 ? (exact.some(isDev) ? ', marked dev' : ', not marked dev') : '';
  const version = planted.version ? '' : ` ${[...new Set(exact.map((c) => c.version))].join(', ')}`;
  return `yes${version}${dev}`;
};

const head = ['#', 'Planted', ...loaded.map((sbom) => `${sbom.label} (${sbom.target})`)];
console.log(`| ${head.join(' | ')} |`);
console.log(`| ${head.map(() => '---').join(' | ')} |`);
for (const planted of PLANTED) {
  console.log(`| ${[planted.id, planted.what, ...loaded.map((sbom) => cell(planted, sbom))].join(' | ')} |`);
}

if (detail) {
  for (const planted of PLANTED) {
    console.log(`\n${planted.id}. ${planted.what}`);
    for (const sbom of loaded) {
      const found = matches(planted, sbom.components);
      if (found.length === 0) {
        console.log(`  ${sbom.file}: no entry`);
        continue;
      }
      for (const component of found) {
        const places = where(component).slice(0, 3).join(', ');
        const dev = isDev(component) ? ' [dev]' : '';
        console.log(`  ${sbom.file}: ${component.type} ${component.name}@${component.version}${dev} ${component.purl ?? ''} ${places}`);
      }
    }
  }
}
