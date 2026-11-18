# Note 002: SCA on two dependencies

Lab for the note [What is SCA? Two dependencies, 51 packages, 8 vulnerable](https://www.appseclab.io/blog/002-what-is-sca/).

A manifest that declares two old npm packages is resolved into a lockfile and checked with `npm audit`. The lab counts how many packages the two became, how many of them have known advisories, and what changes when the two versions are raised to fixed ones.

The `package.json` in this folder is vulnerable on purpose: express 4.17.1 and lodash 4.17.20 both have published advisories. It is a specimen to audit. It is not meant to be installed or copied into a project, and none of the commands below downloads or runs a package.

## What is in the folder

| File | What it is |
| --- | --- |
| `package.json` | The manifest, exactly as the note prints it |
| `package-lock.json` | The tree npm resolved from it on 18 November 2026. It is what `npm audit` read |
| `count.cjs` | Reads `npm audit --json` and prints the table of the note |
| `fixed/package.json` | The same manifest with express 4.22.3 and lodash 4.18.1, the note's second step |
| `fixed/package-lock.json` | Its tree, resolved on 18 November 2026 |
| `results.txt` | Our run on the specimen |
| `results-fixed.txt` | Our run on the fixed versions |

## Run it

It needs Node.js 22.22.0 with npm 11.12.0, the versions the note used, and the network: npm asks the public registry for the tree and for the advisories.

The specimen:

```bash
npm install --package-lock-only --ignore-scripts
npm audit
npm audit --json | node count.cjs
```

With `package-lock.json` in place, the first command keeps the versions it lists. Delete the file first to resolve the tree again from nothing.

`npm audit fix`, to see that it changes nothing. Keep `--package-lock-only`: without it npm installs the tree into `node_modules`.

```bash
shasum -a 256 package-lock.json
npm audit fix --package-lock-only --ignore-scripts
shasum -a 256 package-lock.json
```

The inventory, as a count of the components in a CycloneDX SBOM:

```bash
npm sbom --sbom-format cyclonedx --package-lock-only \
  | node -p 'JSON.parse(require("fs").readFileSync(0, "utf8")).components.length'
```

The fixed versions:

```bash
cd fixed
npm install --package-lock-only --ignore-scripts
npm audit
npm audit --json | node ../count.cjs
```

Each command takes a second or two.

## Reading the output

`npm install` ends with `audited 52 packages`: the 51 of the lockfile and the project itself. `npm audit` ends with `8 vulnerabilities (3 low, 5 high)`, which counts vulnerable packages, and exits with code 1 because it found some. That is the expected result here.

The plain report of `npm audit` prints 17 advisories. The note counts 20, from the JSON report. The other three belong to express (two) and serve-static (one): npm lists those two packages under another one, with the lines "Depends on vulnerable versions of", and leaves their own advisories out. The JSON report has all 20, and `count.cjs` prints them as the note's table:

| Package | Declared | Advisories | Worst of its own | npm rating |
| --- | --- | --- | --- | --- |
| lodash 4.17.20 | yes | 4 | high | high |
| express 4.17.1 | yes | 2 | moderate | high |
| qs 6.7.0 | no | 5 | high | high |
| body-parser 1.19.0 | no | 3 | high | high |
| path-to-regexp 0.1.7 | no | 3 | high | high |
| cookie 0.4.0 | no | 1 | low | low |
| send 0.17.1 | no | 1 | low | low |
| serve-static 1.14.1 | no | 1 | low | low |

Six of the eight are packages the manifest never names. npm rates express high because of the packages it brings in: its own two advisories are moderate at worst.

`npm audit fix` prints the same report again and leaves the lockfile as it was: the two hashes are equal. The SBOM has 51 components.

In `fixed`, `npm audit` prints `found 0 vulnerabilities` and the lockfile has 69 packages.

## Limits

- Advisories change over time. New ones are published every week and one can be withdrawn, so a later run can count more, or fewer, than 20. The note ran on 7 November 2026 and the results here are from 18 November 2026: every number the note reports came out the same.
- The lockfiles are the trees of 18 November 2026. express 4.17.1 pins the six vulnerable packages it brings in at exact versions, but 15 of its other dependencies are ranges, and so are 24 of the 31 dependencies of express 4.22.3. A tree resolved on another day can hold newer versions, another count of packages, and in `fixed` a new advisory.
- `npm audit fix` runs with `--package-lock-only --ignore-scripts`, as in the note, so that it downloads nothing. `npm audit fix --force` was not run, as in the note.
- Without `--package-lock-only`, `npm sbom --sbom-format cyclonedx` stops with `ESBOMPROBLEMS` in a folder that has a lockfile and no `node_modules`. The failed call is in [results.txt](results.txt).
- `fixed/package.json` is not printed in the note. It is the specimen with the two versions the note names.
- Another npm version can word the report differently.
- One line of `results.txt` was edited: npm printed the full path of its log file, and the folder above `.npm` was replaced with `.`.
