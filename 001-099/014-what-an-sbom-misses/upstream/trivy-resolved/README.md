# Upstream: Trivy and the lockfile's `resolved` address

A side finding of note 014. In the lab, is-number comes from a URL on GitHub, and every tool writes it as `pkg:npm/is-number@7.0.0`, the identifier of the registry's package. Trivy 0.76.2 reads the address from the lockfile and none of its built-in formats writes it. This folder holds the smallest case that shows it and the report sent to the Trivy project.

**Status: posted on 25 November 2026 as [discussion 11739](https://github.com/aquasecurity/trivy/discussions/11739)**, by the founder, in the Ideas category (label `kind/feature`). No answer yet.

## What is in the folder

| File | What it is |
| --- | --- |
| `tarball/` | A manifest and its lockfile: is-number from a tarball address on GitHub, ms from the registry |
| `git/` | The same with is-number from git |
| `run.sh` | Runs Trivy on both in CycloneDX, SPDX and its own JSON, counts the lines that carry the address, and prints it with a custom template |
| `results.txt` | Our run of `run.sh` on 25 November 2026 |
| `discussion.md` | The text of the report, as it went into the form |

Nothing is installed or executed: the lockfiles were written with `npm install --package-lock-only --ignore-scripts`, and Trivy reads them with the network off.

```bash
sh run.sh
```

## What the run shows

- The address is in the lockfile and in none of the three outputs (`lines with github.com: 0`), for the tarball and for git.
- A custom template (`--format template`) prints it, so Trivy still holds it when it writes the report.
- Read in the source at tag `v0.76.2`: the npm parser stores it in `Package.ExternalReferences`, the field is tagged `json:"-"`, and the SBOM encoder does not read it. The pnpm and Go module parsers fill the same field; those two were read, not run.

The table, SARIF, `github` and `cosign-vuln` formats were also run once on the tarball case and do not carry the address either. They are not in `run.sh`.

## How it was posted

Trivy takes reports as [discussions](https://trivy.dev/docs/latest/community/contribute/discussion/); an issue opened by someone outside the project is closed at once.

- **Category:** Ideas. Trivy never promised this field, so it is a request, not a defect. The form has one text box, Description, which took the whole of `discussion.md`.
- **Title:** `SBOM: an npm dependency from a URL or git is written like the registry package ("resolved" is parsed, then dropped)`
- **Target:** Filesystem. **Scanner:** left empty, none of the four applies.

Trivy's contribution guide has no rule about AI-assisted reports (read on 25 November 2026). The last section of the text says how it was prepared anyway, as in the cdxgen report of note 011.

## Checked before writing

- Thirteen searches of the project's discussions and eight of its issues and pull requests (`resolved`, `download_url`, `vcs_url`, external references, npm from git or a tarball): no report of this.
- Nearest subjects: [issue 10505](https://github.com/aquasecurity/trivy/issues/10505), a wider proposal that would compare `resolved` with the configured registries, closed at once for being an issue; [discussion 9786](https://github.com/aquasecurity/trivy/discussions/9786), about the `scope` of development dependencies; and, in Syft, [pull request 5230](https://github.com/anchore/syft/pull/5230) for the same lockfile field.
