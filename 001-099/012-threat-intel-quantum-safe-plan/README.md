# Note 012: the cryptography attackers exploit

Lab for the note [Threat intel for a quantum-safe plan: 14 of 1,792](https://www.appseclab.io/blog/012-threat-intel-quantum-safe-plan/).

CISA's Known Exploited Vulnerabilities (KEV) catalog lists vulnerabilities with evidence of exploitation. This lab counts the entries whose weakness is one of the 32 that the OWASP Top 10:2025 maps to A04 Cryptographic Failures, to see how much of what attackers exploit is a failure of cryptography, and of what kind.

## What is in the folder

| File | What it is |
| --- | --- |
| `kev-crypto.mjs` | The script, exactly as the note prints it. It reads the catalog of the day |
| `kev-report.mjs` | A longer version: the same count, broken down by weakness and by year, on the catalog of the day or on a saved copy |
| `a04-cwes.json` | The 32 CWEs of A04 Cryptographic Failures with their names, as the OWASP page lists them, read on 24 November 2026 |
| `kev-2026-11-23.json` | The catalog as `cisa.gov` served it on 24 November 2026: version 2026.11.23, 1,792 entries |
| `results.txt` | Our run of both scripts |

## Run it

It needs Node.js 22 or later and nothing else. Nothing is installed.

The script of the note, on today's catalog. It makes one request, to `www.cisa.gov`:

```bash
node kev-crypto.mjs
```

The numbers of the note, from the saved copy, with no network:

```bash
node kev-report.mjs --file kev-2026-11-23.json
```

As a check to schedule: it lists the entries added since a day and exits with code 1 when there is one.

```bash
node kev-report.mjs --since 2026-11-01
```

## Reading the output

On the saved copy the report prints 1,792 entries, 1,617 with at least one CWE, and 14 with a CWE of A04: 0.78% of the catalog. Two of the 14 are marked as used in ransomware campaigns.

| CWE | Entries | Name |
| --- | --- | --- |
| CWE-347 | 6 | Improper Verification of Cryptographic Signature |
| CWE-326 | 4 | Inadequate Encryption Strength |
| CWE-321 | 1 | Use of Hard-coded Cryptographic Key |
| CWE-324 | 1 | Use of a Key Past its Expiration Date |
| CWE-330 | 1 | Use of Insufficiently Random Values |
| CWE-340 | 1 | Generation of Predictable Numbers or Identifiers |

The note groups the 14 by what failed. That grouping is our reading of the descriptions CISA and the NVD give, not something the script computes:

| What failed | Entries |
| --- | --- |
| A signature that was not checked, or was checked wrongly | CVE-2020-1464, CVE-2020-2021, CVE-2022-20703, CVE-2025-47827, CVE-2025-59718, CVE-2026-48558, CVE-2026-5430 |
| Encryption too weak for the parameters it protected | CVE-2017-11317, CVE-2017-1000486, CVE-2018-15811, CVE-2018-18325 |
| A value that could be predicted | CVE-2020-1472, CVE-2026-118527 |
| A key shipped inside the product | CVE-2025-30406 |

CVE-2025-47827 carries CWE-324 in the catalog; its description says a module improperly verifies a signature, so it is in the first group.

"Not in A04, shown apart" at the end of the report counts two groups the OWASP Top 10:2025 files under A07 Authentication Failures: certificate validation (CWE-295, 297, 298, 299), 5 entries, and hard-coded credentials (CWE-259, 798), 11 entries. No entry is in two of the three groups, so with them the count is 30, or 1.67% of the catalog.

## Limits

- The count depends on the CWE each entry carries in the catalog. 175 entries carry none. Some labels are rough: the catalog names CVE-2026-5430 a path traversal and tags it CWE-347, and CVE-2026-118527 is about predictable file names.
- "Cryptographic" here means "in OWASP's list for A04". Another list gives another number. The 32 are in `a04-cwes.json` so the choice is visible.
- The catalog grows. `kev-crypto.mjs` on a later day prints larger totals. The 14 of the note stay in the list unless CISA removes one.
- The catalog lists exploited vulnerabilities in products. Recording encrypted traffic to decrypt it later exploits no vulnerability and so cannot appear in it.
- The note also says that no technique of MITRE ATT&CK 20.1 mentions harvest now, decrypt later or the word quantum. That was a search of the Enterprise collection (706 techniques and sub-techniques that are not revoked or deprecated) on 24 November 2026, and it is not part of this folder.

## Credits

The [KEV catalog](https://www.cisa.gov/known-exploited-vulnerabilities-catalog) is CISA's, distributed under the [Creative Commons 0 1.0 license](https://github.com/cisagov/kev-data/blob/develop/LICENSE); `kev-2026-11-23.json` is an unchanged copy. Links inside the catalog go to third parties and follow their terms. Keeping a copy here is not an endorsement by CISA or DHS.

The list of CWEs in `a04-cwes.json` is from the [OWASP Top 10:2025, A04 Cryptographic Failures](https://top10.owasp.org/2025/A04_2025-Cryptographic_Failures/), by the OWASP Top 10 Team, which the page licenses under Creative Commons Attribution 3.0 Unported. CWE is maintained by MITRE.

The scripts, the count and the grouping are ours.
