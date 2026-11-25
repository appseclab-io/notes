### Summary

When an npm dependency is installed from a tarball address or from git, `package-lock.json` records where it came from in `resolved`. Trivy parses that field and no built-in output format writes it. The component comes out as `pkg:npm/is-number@7.0.0`, the identifier of the package with that name and version on the npm registry, and nothing in the SBOM says it came from somewhere else.

### To reproduce

`package.json`:

```json
{
  "name": "resolved-demo",
  "version": "1.0.0",
  "private": true,
  "dependencies": {
    "is-number": "https://github.com/jonschlinkert/is-number/archive/refs/tags/7.0.0.tar.gz",
    "ms": "2.1.3"
  }
}
```

```bash
npm install --package-lock-only --ignore-scripts
trivy fs --format cyclonedx --quiet . > sbom.json
```

The lockfile has both origins:

```json
"node_modules/is-number": {
  "version": "7.0.0",
  "resolved": "https://github.com/jonschlinkert/is-number/archive/refs/tags/7.0.0.tar.gz",
```

```json
"node_modules/ms": {
  "version": "2.1.3",
  "resolved": "https://registry.npmjs.org/ms/-/ms-2.1.3.tgz",
```

### Actual result

The two components have the same shape, and the GitHub address is nowhere in the file:

```json
{
  "bom-ref": "pkg:npm/is-number@7.0.0",
  "type": "library",
  "name": "is-number",
  "version": "7.0.0",
  "licenses": [{ "license": { "id": "MIT" } }],
  "purl": "pkg:npm/is-number@7.0.0",
  "properties": [
    { "name": "aquasecurity:trivy:PkgID", "value": "is-number@7.0.0" },
    { "name": "aquasecurity:trivy:PkgType", "value": "npm" }
  ]
}
```

The same holds for `--format spdx-json` (`"downloadLocation": "NONE"`) and for `--format json --scanners license --list-all-pkgs`. A git dependency (`git+https://github.com/jonschlinkert/is-number.git#7.0.0`, locked as `git+ssh://git@github.com/jonschlinkert/is-number.git#98e8ff1d…`) gives the same component.

Trivy still holds the value when it writes the report. A custom template prints it:

```bash
trivy fs --format template --scanners license --list-all-pkgs --quiet \
  --template '{{ range . }}{{ range .Packages }}{{ .Name }}: {{ .ExternalReferences }}{{ "\n" }}{{ end }}{{ end }}' .
```

```
is-number: [{other https://github.com/jonschlinkert/is-number/archive/refs/tags/7.0.0.tar.gz}]
ms: [{other https://registry.npmjs.org/ms/-/ms-2.1.3.tgz}]
```

### Why it matters

The purl type definition for npm has `https://registry.npmjs.org/` as its [default repository](https://github.com/package-url/purl-spec/blob/34201af8a6d3e3e584218cb10981448fc6818d36/types/npm-definition.json), so `pkg:npm/is-number@7.0.0` reads as the registry's package. A fork or a patched copy under the same name and version is a different artifact, and whoever matches advisories or reviews origin from the SBOM cannot tell the two apart.

### Where it happens

- The npm parser reads `resolved` into `ExternalReferences`: [`parse.go#L144-L150`](https://github.com/aquasecurity/trivy/blob/v0.76.2/pkg/dependency/parser/nodejs/npm/parse.go#L144-L150) and [`L188`](https://github.com/aquasecurity/trivy/blob/v0.76.2/pkg/dependency/parser/nodejs/npm/parse.go#L188).
- The field is tagged `json:"-"`: [`package.go#L210`](https://github.com/aquasecurity/trivy/blob/v0.76.2/pkg/fanal/types/package.go#L210).
- The SBOM encoder builds the component without it: [`encode.go#L414-L536`](https://github.com/aquasecurity/trivy/blob/v0.76.2/pkg/sbom/io/encode.go#L414-L536).

Tests aside, I found no reader of `Package.ExternalReferences` in `pkg/` at v0.76.2. The pnpm and Go module parsers fill the same field with a `vcs` reference; I read that in the code and did not test those two.

### Suggestion

Write the reference when there is one, at least when it is not under the default registry:

- CycloneDX: an `externalReferences` entry on the component, `distribution` for a tarball address and `vcs` for git.
- SPDX: the package's `downloadLocation`.
- Possibly the purl qualifier `download_url` or `vcs_url`, which the purl specification [recommends](https://github.com/package-url/purl-spec/blob/main/docs/specification/standard/Annex-B-Recommended-Qualifiers.md) for a direct download and for a version control location. That changes the identifier, so it may be better as a second step.

### Related

- anchore/syft#5230 proposes carrying the same lockfile field into Syft's CycloneDX output.
- #10505 proposed comparing `resolved` with the configured registries to detect dependency confusion. This request is narrower: write what is already parsed.

I searched issues, pull requests and discussions for `resolved`, `download_url`, `vcs_url`, external references and npm dependencies from git or a tarball, and found no report of this.

### Environment

- Trivy 0.76.2, from the image `aquasec/trivy:0.76.2` (linux/arm64), with the network off and the folder mounted read-only
- npm 11.12.0 and Node.js 22.23.4 for the lockfile

### AI use

Prepared with an AI assistant (Claude Code): it ran the tests above, read the source and drafted this text. I reviewed it before posting and I am responsible for it.
