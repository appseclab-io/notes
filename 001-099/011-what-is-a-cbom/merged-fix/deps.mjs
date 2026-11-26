// Compares the dependencies of two package.json files: the cdxgen installed in
// the image, and the cdxgen source about to run with the image's node_modules.
// A dependency the source dropped is harmless. One it added or changed is not
// in the image, so the run stops.
import { readFileSync } from 'node:fs';

const [installedPath, sourcePath] = process.argv.slice(2);
const read = (file) => JSON.parse(readFileSync(file, 'utf8'));
const installed = read(installedPath);
const source = read(sourcePath);

console.log(`The image has cdxgen ${installed.version}; the source is ${source.version}.`);

let missing = 0;
for (const field of ['dependencies', 'optionalDependencies']) {
  const before = installed[field] || {};
  const after = source[field] || {};
  for (const name of Object.keys(before).sort()) {
    if (!(name in after)) {
      console.log(`${field}: the source no longer uses ${name}`);
    }
  }
  for (const name of Object.keys(after).sort()) {
    if (before[name] !== after[name]) {
      missing += 1;
      console.log(`${field}: the source wants ${name} ${after[name]}, the image has ${before[name] ?? 'none'}`);
    }
  }
}

if (missing > 0) {
  console.log(`Not running: ${missing} of the source's dependencies are not in the image.`);
  process.exit(1);
}
console.log('Every dependency of the source is in the image.');
