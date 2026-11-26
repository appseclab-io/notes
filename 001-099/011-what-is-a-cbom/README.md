# Note 011: what a CBOM generator lists

Lab for the note [What is a CBOM? Eight uses of cryptography, four listed](https://www.appseclab.io/blog/011-what-is-a-cbom/).

A cryptography bill of materials (CBOM) lists the algorithms, keys, certificates and protocols a piece of software uses. This lab runs a CBOM generator, cdxgen, on a 48-line Node.js app with eight planted uses of cryptography, then on a second file with 32 ways to ask Node.js for cryptography. It shows what the generator lists, and what it does not see.

## What is in the folder

| File | What it is |
| --- | --- |
| `app/app.js` | The app, exactly as the note prints it |
| `app/package.json`, `app/package-lock.json` | Its two dependencies, `jsonwebtoken` 9.0.3 and `bcryptjs` 3.0.3, with fixed versions |
| `cbom.json` | The CBOM of the app, from our run |
| `results.txt` | Our run on the app: the tool's output, the list of entries and the validation line |
| `probe/probe.js` | 32 calls that ask Node.js for cryptography, one per line |
| `probe/package.json`, `probe/package-lock.json` | Its one dependency, `jsonwebtoken` 9.0.3 |
| `probe-cbom.json` | The CBOM of the probe, from our run |
| `results-probe.txt` | Our run on the probe |

Nothing here is installed or started. The scanner reads the two files. `probe.js` must never be run: three of its names are ones Node.js 22 does not accept.

## Run it

It needs Docker, `jq`, and the image of cdxgen 13.8.0, the version the note used. The image is about 16.6 GB on disk. The tag below is for Apple silicon and other ARM machines; on Intel or AMD use `v13.8.0-amd64`, which we did not run. Pulling the image needs the network. The scans do not: they run with the network off and the source mounted read-only.

The app, from this folder:

```bash
docker run --rm --network none \
  -v "$PWD/app:/app:ro" -v "$PWD:/out" \
  ghcr.io/cdxgen/cdxgen:v13.8.0-arm64 \
  --include-crypto --spec-version 1.7 \
  -o /out/cbom.json /app
```

The entries it found, with the line each one came from:

```bash
jq -r '.components[] | select(.type == "cryptographic-asset") | .name as $n | .properties[] | select(.name == "cdx:crypto:sourceLocation") | "\(.value)\t\($n)"' cbom.json | sort -t: -k2 -n
```

The probe is the same pair of commands with `probe` in place of `app` and `probe-cbom.json` in place of `cbom.json`.

The schema check, with the validator that ships in the same image:

```bash
docker run --rm --network none --entrypoint cdx-validate \
  -v "$PWD:/out:ro" ghcr.io/cdxgen/cdxgen:v13.8.0-arm64 \
  -i /out/cbom.json --no-deep --strict | head -1
```

Each scan takes about ten seconds. Beside the output, cdxgen leaves its working files (`js-app.atom` and three `js-*.slices.json`). They are not kept here and can be deleted.

## Reading the output

The app. The tool reports `16 npm, 4 cryptographic-asset`: the 16 packages of the lock file and four algorithms.

| Use in `app.js` | Line | In `cbom.json` |
| --- | --- | --- |
| 1. `createHash('md5')` | 8 | `md5`, primitive `hash` |
| 2. `createCipheriv('aes-256-gcm', …)` | 14 | Nothing |
| 3. `generateKeyPairSync('rsa', { modulusLength: 2048 })` | 19 | `rsa`, with empty `algorithmProperties`: no key size |
| 4. `createECDH('prime256v1')` | 23 | Nothing |
| 5. `createHash(process.env.DIGEST \|\| 'sha1')` | 30 | `sha-1`, the default, with nothing about the environment |
| 6. `jwt.sign(…, { algorithm: 'RS256' })` | 35 | `sha256WithRSAEncryption`, primitive `signature` |
| 7. `bcrypt.hash(password, 12)` | 40 | Nothing as cryptography. `bcryptjs` 3.0.3 is one of the 16 libraries |
| 8. `https.get(…)` | 45 | Nothing |

No entry has `nistQuantumSecurityLevel`, and no item of `dependencies` refers to a cryptographic asset: the property `cdx:crypto:sourceLocation` is the only link between an algorithm and the app.

The probe. 16 of the 32 lines are listed.

| Lines of `probe.js` | Call | Listed as |
| --- | --- | --- |
| 13, 14, 15 | `createHash` with the name in the call, in a constant, through a renamed import | `sha-256`, `sha-512`, `sha3-256` |
| 16 | `createHash` with the name passed through a function | Not listed |
| 17 | `createHash` with a name known only when the app runs | Not listed |
| 18 | `createHmac('sha256', …)` | `sha-256` |
| 20, 21 | `pbkdf2Sync`, `hkdfSync` with `sha256` | `sha-256` |
| 22 | `scryptSync` | Not listed |
| 24, 25 | `createCipheriv` with `aes-256-gcm`, `id-aes256-GCM` | Not listed |
| 26 | `createCipheriv('aes256-GCM', …)` | `aes256-GCM` |
| 27, 28 | `createCipheriv` with `aes-256-cbc`, `chacha20-poly1305` | Not listed |
| 30 | `createSign('RSA-SHA256')` | Not listed |
| 31 | `createSign('sha256WithRSAEncryption')` | `sha256WithRSAEncryption` |
| 32 | `sign('sha256', …)` | `sha-256` |
| 34 | `generateKeyPairSync('rsa', …)` | `rsa`, no key size |
| 35 | `generateKeyPairSync('ec', { namedCurve: 'prime256v1' })` | Not listed |
| 36, 37, 38, 39 | `generateKeyPairSync` with `ed25519`, `x25519`, `ml-kem-768`, `ml-dsa-65` | `ed25519`, `x25519`, `ml-kem-768`, `ml-dsa-65` |
| 40, 41 | `createECDH`, `diffieHellman` | Not listed |
| 43 | `subtle.digest('SHA-256', …)` | `sha-256` |
| 44, 45 | `subtle.encrypt` with AES-GCM, `subtle.generateKey` with ECDSA | Not listed |
| 47 | `jwt.sign` with `ES256` | `ecdsaWithSHA256` |
| 48, 49 | `jwt.sign` with the library's default, `jwt.verify` | Not listed |
| 51 | `tls.connect` with `ecdhCurve: 'X25519MLKEM768'` | Not listed |

Three of the 16 listed lines hold a name that Node.js 22.23.4 refuses. `aes256-GCM` is an unknown cipher to it: Node.js takes cipher names from OpenSSL, which spells this one `aes-256-gcm` or `id-aes256-GCM`. The key types `ml-kem-768` and `ml-dsa-65` exist from Node.js 24.7.0.

Why a call the tool reads can still be missing: in cdxgen 13.8.0 the JavaScript analyzer records the name it finds (`lib/inventory/analyzer.js`), and the CBOM step normalizes that name and keeps it only when `data/crypto-oid.json` has an entry for it (`lib/inventory/cbomutils.js`). The table has `aes256-GCM` and `sha256WithRSAEncryption`, and has no `aes-256-gcm`, `RSA-SHA256` or `scrypt`.

`schemaValid=true` on the validation line is the schema check. The other numbers on that line come from the validator's compliance checks, which the note does not use.

## Limits

- One version of one tool, on two small files, on 24 November 2026. cdxgen had a release in each of the four weeks before the run, so a later version can list more.
- The names that are read and then left out, and the RSA key size, were reported to the project on 24 November 2026: [cdxgen issue 4896](https://github.com/cdxgen/cdxgen/issues/4896). It was fixed: on 26 November 2026 the maintainer merged [pull request 4904](https://github.com/cdxgen/cdxgen/pull/4904), which closes the issue — it maps the OpenSSL spellings, adds the object identifiers the table lacked, and records the RSA modulus and a key's named curve. It is marked for the 13.9.0 release, so the results above are the 13.8.0 snapshot.
- Only source-level reading was tested. cdxgen can also trace a running command (`tracebom`) and read certificates and Java keystores; none of that was run.
- The `cbom` shortcut of the same image (`--entrypoint cbom`), which adds `--evidence --deep`, listed the same four entries for the app. So did the scan with the dependencies installed in `node_modules`. Those two runs are not kept as files here.
- The tag `v13.8.0-arm64` names one build. The tag `v13.8` moves with each patch release.
- At the top of its output the tool prints an environment audit that names a variable of its own image. It is not about this lab.
- The results files are the tool's raw output, with its own box drawing and symbols.

## Credits

[cdxgen](https://github.com/cdxgen/cdxgen) is an OWASP Foundation project, written by Prabhu Subramanian and its contributors, under the Apache 2.0 license. The CBOM format is part of [OWASP CycloneDX](https://cyclonedx.org/capabilities/cbom/), published as ECMA-424. Neither is ours. The app, the probe and the tables above are.
