# Note 014: what an SBOM misses

Lab for the note [What does an SBOM miss? Ten components, five listed](https://www.appseclab.io/blog/014-what-an-sbom-misses/).

A small Node.js app with a Dockerfile has ten third-party components in it, each one brought in a different way. Four free SBOM generators read the folder, two of them also read the container image built from it, and `check.mjs` says which of the ten each SBOM lists. The note is the continuation of [note 002](../002-what-is-sca/), which said in one paragraph that code outside the manifest is invisible to an SBOM, and did not measure it.

The app in `app/` is a specimen. Four of the components it carries have published advisories on purpose: lodash 4.17.20, jQuery 3.4.1, axios 0.21.1 and jq 1.7.1. Do not copy it into a project. Nothing here runs it: no package is executed (`--ignore-scripts`), the server is never started, and the image is built, saved to a file and scanned, never run.

## What we planted

| # | Component | How it got in | Where |
| --- | --- | --- | --- |
| 1 | debug 4.3.1 | Declared, from the npm registry | `app/package.json` |
| 2 | ms 2.1.2 | A dependency of debug | `app/package-lock.json` only |
| 3 | mime 2.5.2 | A development dependency | `app/package.json` |
| 4 | is-number 7.0.0 | Declared, downloaded from a URL on GitHub | `app/package.json` |
| 5 | lodash 4.17.20 | Copied in as one file, no manifest | `app/vendor/lodash.js` |
| 6 | jQuery 3.4.1 | A minified file, committed | `app/public/js/jquery-3.4.1.min.js` |
| 7 | axios 0.21.1 | Loaded from a CDN by a `<script>` tag | `app/public/index.html` |
| 8 | escape-html 1.0.3 | One function, copied into a file of ours | `app/src/text.js` |
| 9 | curl | A system package, `apk add` | `app/Dockerfile` |
| 10 | jq 1.7.1 | A binary downloaded with `curl` | `app/Dockerfile` |

`check.mjs` also looks for the runtime, Node.js 22.23.4, which is in the image and in no manifest.

## What is in the folder

| File | What it is |
| --- | --- |
| `app/` | The specimen: manifest, lockfile, a 35-line server, the planted files and the Dockerfile |
| `run.sh` | Resolves the lockfile, builds and saves the image, writes the six SBOMs and prints the table |
| `check.mjs` | Reads the six SBOMs and prints, for each planted component, whether each one lists it |
| `extras.sh` | Three checks behind sentences of the note, beyond the table |
| `sbom-npm.json`, `sbom-cdxgen.json`, `sbom-syft-dir.json`, `sbom-trivy-dir.json` | The SBOMs of the folder |
| `sbom-syft-image.json`, `sbom-trivy-image.json` | The SBOMs of the image |
| `results.txt` | Our run of `run.sh` on 24 November 2026 |
| `results-extras.txt` | Our run of `extras.sh` |
| `upstream/trivy-resolved/` | A side finding in its smallest case, and the report sent to the Trivy project ([discussion 11739](https://github.com/aquasecurity/trivy/discussions/11739)) |

## Run it

It needs Docker, and Node.js 22 with npm 11. Ours were Docker 29.9.3, Node.js 22.23.4 and npm 11.12.0, on an Apple silicon Mac. The tools run from pinned images: cdxgen 13.8.0 (`ghcr.io/cdxgen/cdxgen:v13.8.0-arm64`, about 16.6 GB), Syft 1.57.2 (`anchore/syft:v1.57.2`) and Trivy 0.76.2 (`aquasec/trivy:0.76.2`). The base image is `node:22.23.4-alpine`.

```bash
sh run.sh
```

The network is used to resolve the lockfile, to ask `npm audit` and to build the image, which downloads the base image, the Alpine package and the jq binary. The scans in Docker run with `--network none` and read the source or the image read-only.

The image is saved to a file of about 65 MB, by default in the system's temporary folder. `IMAGE_TAR=/path/to/file.tar sh run.sh` puts it somewhere else. Keep it out of the repository.

On Intel or AMD, change two things first: the cdxgen tag to `v13.8.0-amd64` in `run.sh` and `extras.sh`, and the asset to `jq-linux-amd64` in `app/Dockerfile`.

Just the table, from the SBOMs already in the folder:

```bash
node check.mjs
node check.mjs --detail
```

## Reading the output

`npm audit` says `found 0 vulnerabilities`. It reads the lockfile, and the four components with advisories are not in it.

`check.mjs` first prints how many entries each SBOM has. The counts are not comparable between tools: Syft counts the app itself as a library and lists files, and Trivy lists the lockfile as an application.

| SBOM | Entries |
| --- | --- |
| `sbom-npm.json` | 4 libraries |
| `sbom-cdxgen.json` | 5 libraries |
| `sbom-syft-dir.json` | 4 libraries, 1 file |
| `sbom-trivy-dir.json` | 3 libraries, 1 application |
| `sbom-syft-image.json` | 230 libraries, 297 files, 2 applications, 1 operating system |
| `sbom-trivy-image.json` | 230 libraries, 1 operating system |

Then the table, one row per planted component. A cell says `yes` when the SBOM has an entry with that name and that version:

| # | Planted | npm sbom | cdxgen | Syft | Trivy | Syft, image | Trivy, image |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 1 | debug 4.3.1, declared | yes | yes | yes | yes | yes | yes |
| 2 | ms 2.1.2, transitive | yes | yes | yes | yes | yes | yes |
| 3 | mime 2.5.2, development | yes, marked dev | yes, marked dev | no | no | no | no |
| 4 | is-number 7.0.0, from a URL | yes | yes | yes | yes | yes | yes |
| 5 | lodash 4.17.20, copied file | no | no | no | no | no | no |
| 6 | jQuery 3.4.1, minified file | no | yes | no | no | no | no |
| 7 | axios 0.21.1, from a CDN | no | no | no | no | no | no |
| 8 | escape-html 1.0.3, copied function | no | no | no | no | no | no |
| 9 | curl, system package | no | no | no | no | yes, 8.23.0-r0 | yes, 8.23.0-r0 |
| 10 | jq 1.7.1, downloaded binary | no | no | no | no | yes | no |
| | Node.js 22.23.4, the runtime | no | no | no | no | yes | no |

Of the ten: `npm sbom` lists four, cdxgen five, Syft and Trivy three each on the folder, Syft five and Trivy four on the image. The six files together list seven. Components 5, 7 and 8 are in none.

In the image, mime is absent because it is not there: the build runs `npm ci --omit=dev`. The two copied files (5 and 6) are in the image and have no entry.

`--detail` adds, for every match, the version, the package URL and where the tool says it found the component. It shows three things the table does not:

- **debug and ms appear twice in the image SBOMs.** Once in `/app/node_modules`, at our versions, and once inside npm itself (`/usr/local/lib/node_modules/npm`), at 4.4.3 and 2.1.3. Of the 230 libraries of the image, 196 are packages inside npm, 28 are Alpine packages, 4 are in `/app` (our three and the app itself), and corepack and yarn are one each.
- **is-number is written as `pkg:npm/is-number@7.0.0` by every tool**, the identifier of the registry's package. `npm sbom` and cdxgen keep the GitHub address as a `distribution` reference. Syft and Trivy read it and do not write it to CycloneDX: Syft has it in its own format (`-o syft-json`, field `resolved`), and a [pull request](https://github.com/anchore/syft/pull/5230) to carry it into CycloneDX has been open since August 2026; Trivy 0.76.2 parses it and writes it to neither format, though a custom template can print it (`upstream/trivy-resolved/`, reported in [discussion 11739](https://github.com/aquasecurity/trivy/discussions/11739)).
- **jq and Node.js are `application` entries in Syft's image SBOM**, found by `binary-classifier-cataloger`, with `pkg:generic/` identifiers.

Neither image SBOM names the base image, `node:22.23.4-alpine`, or its digest.

## The three extra checks

```bash
sh extras.sh
```

It needs the image `run.sh` built. Our output is in `results-extras.txt`.

1. **The development dependency, with each tool's switch on.** `SYFT_JAVASCRIPT_INCLUDE_DEV_DEPENDENCIES=true` for Syft and `--include-dev-deps` for Trivy: both then list mime 2.5.2, and neither entry carries anything that says it is a development dependency. CycloneDX has the `scope` field for it, to be read as `required` when missing. Trivy knows the difference (its own JSON says `"Dev": true`) and its maintainers have said that filling `scope` would make sense ([discussion 9786](https://github.com/aquasecurity/trivy/discussions/9786)); Syft's lockfile entry has no such flag.
2. **Why cdxgen listed jQuery.** cdxgen 13.8.0 looks for files whose name ends in `min.js` and takes a name and a version from a `/*!` comment line ([`parseMinJs`](https://github.com/cdxgen/cdxgen/blob/v13.8.0/lib/ecosystems/parsers-js.js#L5084), called from [`jsBom.js`](https://github.com/cdxgen/cdxgen/blob/v13.8.0/lib/cli/jsBom.js#L491)). Three variants, each scanned alone: (a) the same bytes named `jquery-3.4.1.js`, (b) the same name without the first line, (c) `vendor/lodash.js` renamed `lodash.min.js`, whose header is not a `/*!` line. None is listed.
3. **Why Syft listed jq.** Syft 1.57.2 has a [list of binaries it knows](https://github.com/anchore/syft/blob/v1.57.2/syft/pkg/cataloger/binary/classifiers.go), each with a file name pattern and bytes to find; the entry `jq-binary` looks for `**/jq`. The binary copied out of the image is listed under the name `jq` and has no entry under the name `json-tool`. The container is created to copy the file and is never started.

## Limits

- One version of each tool, one small app, one ecosystem, on 24 November 2026. A later version of any of them can list more. This is not a ranking of the tools.
- The image is for arm64. On another architecture the Alpine package versions and the layer digests differ.
- The SBOM files carry timestamps and serial numbers, so a new run writes different bytes. The table is the same: we ran the six scans twice and compared it.
- Trivy and Syft were asked for an SBOM only. We did not run their vulnerability scans, and nothing here says what those would report.
- Component 8 is a function, not a file. No tool here compares source code with known libraries, and the lab does not try one that does.
- `npm audit` asks the public registry, and its answer changes as advisories are published.

## Credits

- `npm sbom` and `npm audit` are part of the [npm CLI](https://github.com/npm/cli), under the Artistic License 2.0.
- [cdxgen](https://github.com/cdxgen/cdxgen) is an OWASP Foundation project by Prabhu Subramanian and its contributors, under the Apache 2.0 license.
- [Syft](https://github.com/anchore/syft) is by Anchore and its contributors, under the Apache 2.0 license.
- [Trivy](https://github.com/aquasecurity/trivy) is by Aqua Security and its contributors, under the Apache 2.0 license.
- `app/vendor/lodash.js` is the file `lodash.js` of lodash 4.17.20 and `app/public/js/jquery-3.4.1.min.js` is `dist/jquery.min.js` of jQuery 3.4.1, both unchanged and under the MIT license; each file carries its own copyright notice.
- `escapeHtml` in `app/src/text.js` is from escape-html 1.0.3, by TJ Holowaychuk, Andreas Lubbe and Tiancheng "Timothy" Gu, under the MIT license.
- debug, ms, mime, is-number and axios are under the MIT license and are not in this repository: the lockfile and one URL name them. jq 1.7.1, also MIT, is downloaded by the image build.
