# Note 013: what a CBOM reads in Java

Lab for the note [What does a CBOM read in Java? 55 cases, 43 listed](https://www.appseclab.io/blog/013-what-a-cbom-reads-in-java/).

Note 011 gave a CBOM generator a Node.js app with eight planted uses of cryptography and counted what it listed. This lab ports the same eight uses to Java and adds a probe of 55 small cases, each one a complete operation that runs. Two tools then read the code: cdxgen, the tool of note 011, and the cryptography plugin for SonarQube. Because the probe runs, there is a ground truth to compare each CBOM with: what the JDK and BouncyCastle did, case by case.

## What is in the folder

| File | What it is |
| --- | --- |
| `app/src/main/java/lab/App.java` | The eight uses of note 011, in Java |
| `app/pom.xml` | Its two dependencies, `java-jwt` 4.6.2 and `jbcrypt` 0.4, with fixed versions |
| `probe/src/main/java/lab/Jca.java` | 27 cases with the cryptography the JDK ships: hashes, MAC, key derivation, ciphers, signatures, key pairs, key agreement, random numbers |
| `probe/src/main/java/lab/Pqc.java` | 4 cases: ML-KEM, ML-DSA and the key encapsulation API, from the JDK |
| `probe/src/main/java/lab/Tls.java` | 5 cases of TLS configuration. None opens a connection or binds a port |
| `probe/src/main/java/lab/Bc.java` | 19 cases with BouncyCastle 1.87: 17 through its light-weight API, 2 through the JCA with BouncyCastle as the provider |
| `probe/src/main/java/lab/Main.java` | Runs the 55 cases once and prints what each one did |
| `probe/pom.xml` | The probe's one dependency, `bcprov-jdk18on` 1.87 |
| `results-run.txt` | Our run of the probe |
| `scan.sh` | Scans the app and the probe with a SonarQube server |
| `cases.mjs` | Lists, case by case, what a CBOM says about the lines of that case |
| `cbom-sonar-app.json`, `cbom-sonar-probe.json` | The CBOMs the SonarQube plugin wrote |
| `results-sonar.txt` | Our scans: the plugin's own count and the output of `cases.mjs` |
| `cbom-cdxgen-app.json`, `cbom-cdxgen-probe.json` | The CBOMs cdxgen 13.8.0 wrote |
| `results-cdxgen.txt` | Our cdxgen runs: the tool's output, the entries and the validation line |
| `upstream/sonar-cryptography/` | The plugin's findings reduced to 22 small cases, with the control on BouncyCastle 1.85.2, their scan and the reports written for the project. Its own README says how to run it |

In the probe a case starts at its `// [ID]` comment and ends at the next one. `J` is the JDK, `P` post-quantum, `T` TLS, `B` BouncyCastle.

The app is compiled, not started. We called its uses 1 to 7 once from a scratch class to check the names; use 8 would reach `api.example.com` and was not called.

## Run it

It needs a JDK that accepts `--release 25`, Maven and Node.js. Ours were Temurin 27, Maven 3.10.0 and Node.js 22, and we ran the probe only on JDK 27. Maven downloads the dependencies from Maven Central the first time.

### The probe

```bash
cd probe && mvn -q compile exec:java
```

It prints one line per case and ends with `55 cases ran, 0 failed`. A key generated in a case is new on every run, so the two ECDSA signature sizes can differ by a byte or two from `results-run.txt`.

### The SonarQube plugin

It needs a SonarQube server you administer. Ours was SonarQube Community Build 26.4.0 with [sonar-cryptography](https://github.com/cbomkit/sonar-cryptography) 1.7.0, installed as its README says: the jar in `extensions/plugins`, then a restart. The 1.7.0 release page carries no jar; it is on the project's package page. On the server:

1. A Java quality profile with the rule *Cryptographic Inventory (CBOM)* active. Without that rule the scan passes and writes no CBOM. Ours was a child of *Sonar way* with the plugin's two Java rules.
2. Two projects on that profile, `notes-java-cbom-app` and `notes-java-cbom-probe`.
3. A token that can run an analysis.

Then, from this folder:

```bash
SONAR_HOST_URL=https://sonar.example.com ./scan.sh
```

It asks for the token without showing it, scans both folders and leaves a `sonar-app.log` and a `sonar-probe.log`. The plugin writes `cbom.json` in each scanned folder; the two files kept here are those, renamed. To read one against the cases:

```bash
node cases.mjs cbom-sonar-probe.json probe
```

The analysis took 7 seconds for the app and 11 for the probe.

### cdxgen

It needs Docker and the image of cdxgen 13.8.0, about 16.6 GB on disk; the tag below is for Apple silicon and other ARM machines. For Java it runs Maven inside the folder it scans, so it gets a copy of the sources, and the network stays on for Maven Central:

```bash
mkdir -p /tmp/note-013-app && cp -R app/pom.xml app/src /tmp/note-013-app/
```

```bash
docker run --rm -v /tmp/note-013-app:/app -v "$PWD:/out" \
  ghcr.io/cdxgen/cdxgen:v13.8.0-arm64 \
  --include-crypto --spec-version 1.7 \
  -o /out/cbom-cdxgen-app.json /app
```

```bash
jq -r '.components[] | select(.type == "cryptographic-asset") | "\(.name)\t\(.cryptoProperties.oid)"' cbom-cdxgen-app.json
```

The probe is the same three commands with `probe` in place of `app`. Each scan takes about 30 seconds. Beside the output, cdxgen leaves its working files (`java-app.atom` and three `java-*.slices.json`); they are not kept here and can be deleted.

## Reading the output

### The probe, run

All 55 cases ran on JDK 27 with BouncyCastle 1.87. Three lines of `results-run.txt` that the rest leans on:

- `J04` printed `SHA3-512`: the name came from the caller when the app ran. Nothing in `Jca.java` holds it.
- `J11` asked for `AES` and nothing else. The JDK documents that as AES in ECB mode with PKCS5 padding; the run shows no IV.
- `T05` named nothing and printed `first group X25519MLKEM768`: on this JDK the default TLS offer starts with the hybrid post-quantum key exchange group.

### The app, three readings

The first column is note 011: the same eight uses in Node.js, read by cdxgen. The Java app is a port of that one, not the same file.

| Use | cdxgen 13.8.0, Node.js | cdxgen 13.8.0, Java | sonar-cryptography 1.7.0, Java |
| --- | --- | --- | --- |
| 1. MD5, named in the call | `md5` | Nothing | `MD5`, hash, line 29 |
| 2. AES-GCM, named in the call | Nothing | `aes`, no mode, no size, no line | `AES-128-GCM`, line 36, and its IV, 96 bits |
| 3. RSA key pair, 2048 bits | `rsa`, no key size | Nothing | `RSA-2048` and a 2048-bit key, line 44 |
| 4. ECDH on P-256 | Nothing | Nothing | `EC`, `EC-secp256r1` and `ECDH`, lines 51 to 53 |
| 5. Hash named by the environment, default SHA-1 | `sha-1`, the default | Nothing | Nothing |
| 6. RS256 inside a JWT library | `sha256WithRSAEncryption` | Nothing | Nothing |
| 7. bcrypt inside a library | Nothing; the library is listed | Nothing; the library is listed | Nothing |
| 8. TLS, nothing named | Nothing | Nothing | Nothing |
| Uses listed | 4 | 1 | 4 |

The two fours are not the same four: only uses 1 and 3 are in both.

In use 2 the key arrives from the caller, so nothing in `App.java` fixes its size. The plugin's entry says 128. When we called the method, we passed a 256-bit key.

The plugin's log counts 6 detected assets for the app; the file has 10 components, because key material (the IV, three keys) and the curve are components too. It types the keys of a key pair as `secret-key`.

### The probe, read by the plugin

The file has 81 components. 43 of the 55 cases have at least one algorithm or protocol on their lines, each with its line and the call it was read from.

The 12 cases with nothing:

| Case | What ran | What we can say about why |
| --- | --- | --- |
| J04 | A hash whose name arrives when the app runs | The name is not in the code |
| J08 | HKDF through `javax.crypto.KDF` | The plugin's Java rules at tag 1.7.0 have none for `javax.crypto.KDF` or `javax.crypto.KEM` |
| J26, J27 | `SecureRandom`, named and default | A rule file for it exists (`random/SecureRandomGetInstance.java`); the list the Java rules are built from (`JavaDetectionRules.java`) holds the JCA, BouncyCastle and TLS sets |
| T04 | TLS key exchange group `X25519MLKEM768` set by name | No rule reads `setNamedGroups` |
| T05 | TLS with nothing named | Nothing to read |
| B05, B06 | scrypt and Argon2id, BouncyCastle | Not checked |
| B14 | X25519 key agreement, BouncyCastle | Not checked |
| B15, B16 | ML-KEM-768 and ML-DSA-65, BouncyCastle | The plugin's rules name `org.bouncycastle.pqc.crypto.mlkem` and `…mldsa`. BouncyCastle 1.86 and 1.87 keep these classes under `org.bouncycastle.crypto` only. Measured on 1.85.2, which has both: the old classes are listed (`ML-KEM`, `ML-DSA`, no parameter set), the new ones are not (`upstream/sonar-cryptography/`) |
| B17 | SLH-DSA, BouncyCastle | The Java rules know `SPHINCSPlusSigner`, its name before FIPS 205, and no `SLHDSASigner`. Not listed on 1.85.2 either, in either package |

The JDK's own post-quantum cases are listed: `ML-KEM-768` (P01) and `ML-DSA-65` (P04, and B18 through the JCA with BouncyCastle as the provider), from the name given to `KeyPairGenerator`.

Cases that are listed, where the entry and the run disagree:

| Case | The run | The CBOM |
| --- | --- | --- |
| J09 | AES-GCM with a 256-bit key generated two lines above | The key is `AES-256`; the cipher is `AES-128-GCM` |
| J12 | A 256-bit key built from bytes | The key is `AES-128`. The cipher, named `AES_256/GCM/NoPadding`, is `AES-256-GCM` |
| J16 | AES key wrap, both keys 256 bits | `AES-128`, and nothing about key wrap |
| B07, B08, B19 | AES-GCM with a 256-bit key | `AES-GCM` or `AES-128-GCM`, 128 |
| J24 | Diffie-Hellman, 2048 bits on both sides | `FFDH-2048` for the first key pair, `FFDH-3072` for the second and for the agreement |
| P02 | ML-KEM-1024, the parameter set given in a second call | `ML-KEM-768` |
| J14 | Triple DES, a 192-bit key | `3DES` with 64 as its parameter set |
| J13 | ChaCha20-Poly1305 through the JDK | `ChaCha20`, a stream cipher, and `Poly1305`, a hash. Through BouncyCastle (B09) the same algorithm is one entry, `ChaCha20-Poly1305` |
| J11 | `AES` alone, which the JDK ran in ECB mode | `AES-128`, no mode |
| P03 | DHKEM over X25519 | The X25519 key pair. No entry for the encapsulation |
| T02 | TLS 1.2 and one cipher suite, set on the parameters of an engine | `TLS`, no version, no suite. On a server socket (T03) the version and the suite are listed |
| B12 | ECDSA on `secp256r1` | `ECDSA-SHA-256`, no curve |

Every AES entry whose size is not part of the algorithm name says 128. That is right for the three cases with 16-byte keys (J10, J11, B10) and wrong for the six with 256-bit keys.

What it reads that a name search cannot: the name passed through a method (J03 is `SHA-384`, on the line of the call), key sizes set in a second call (J15, J17, J21), curves (J18, J22), the parameters of RSA-PSS (J19) and a TLS cipher suite taken apart into `AES-256-GCM` and `SHA-384` (T03).

### The probe and the app, read by cdxgen

The app: `5 maven`, the two dependencies and the three they bring, and one cryptographic asset, `aes`. The entry has no property and no evidence, so nothing in the file ties it to line 36: the match is by name.

The probe: six names for 55 cases, `aes`, `aes256-GCM`, `chaCha20Poly1305`, `des`, `hmacSHA` and `sha-256`. None has a location, so they cannot be assigned to cases. No entry names RSA, an elliptic curve, a signature, a key agreement, TLS, ML-KEM or ML-DSA.

Why Java reads so differently from JavaScript in this version: for JavaScript the tool walks the syntax tree and records each call with its line (note 011). For Java it builds data-flow slices, keeps the flows its analyzer tags as cryptography, joins their code into one text and searches that text for every name of its OID table (`collectReachableFrames` in `lib/evinser/evinser.js`, `findCryptoAlgos` in `lib/inventory/cbomutils.js`). A name found that way becomes an entry with an OID and no location.

## Limits

- One version of each tool, on one small app and one probe, on 24 and 25 November 2026.
- What we found in the plugin was reported to its project on 25 November 2026: [issue 572](https://github.com/cbomkit/sonar-cryptography/issues/572) (key sizes and parameter sets), [issue 573](https://github.com/cbomkit/sonar-cryptography/issues/573) (BouncyCastle 1.86 and SLH-DSA) and [issue 574](https://github.com/cbomkit/sonar-cryptography/issues/574) (`KEM`, `KDF`, `SecureRandom`, TLS named groups). Follow them to see whether a later version changes these results.
- The plugin 1.7.0 was built against a newer SonarQube plugin API (13.11) than our server offers (13.5), and against a newer Python analyzer. It declares 9.14 as its minimum, loaded, and both scans ended without an error. A newer server could give other results.
- The reasons in the table of 12 come from reading the plugin's sources at tag 1.7.0, not from its authors; the BouncyCastle rows were also measured. Two rows say "not checked".
- The probe ran only on JDK 27. It compiles with `--release 25`, so every class it uses is part of Java 25; which of its algorithm names an older JDK accepts was not tested.
- BouncyCastle 1.84 and 1.85.2 have the ML-KEM, ML-DSA and SLH-DSA classes under both `org.bouncycastle.crypto` and `org.bouncycastle.pqc.crypto`; 1.86 and 1.87 only under the first. The probe uses 1.87; the control on 1.85.2 is in `upstream/sonar-cryptography/`, scanned on 25 November 2026.
- Only the Java rules of the plugin were tested. It also reads Python and Go.
- cdxgen ran with the network on and a writable copy of the sources. With the network off and the folder read-only, as in note 011, its Maven step failed, it listed the two direct dependencies, and the cryptographic entry was the same `aes`.
- Only source-level reading was tested. cdxgen can also read certificates and Java keystores; none of that was run.
- The fixed keys, salts and nonces of the probe (`Jca.bytes`) are there so that a case names an algorithm and nothing else. They are not a pattern to copy.
- The scanner logs are not kept: they hold the paths and the address of our own server. `results-sonar.txt` has the plugin's lines from them.
- At the top of its output cdxgen prints an environment audit that names a variable of its own image. It is not about this lab.

## Credits

[sonar-cryptography](https://github.com/cbomkit/sonar-cryptography), also called CBOMkit-hyperion, is part of the CBOMkit project and is written by its contributors, under the Apache 2.0 license. [SonarQube Community Build](https://www.sonarsource.com/open-source-editions/sonarqube-community-edition/) is by SonarSource, under the LGPL 3.0. [cdxgen](https://github.com/cdxgen/cdxgen) is an OWASP Foundation project, written by Prabhu Subramanian and its contributors, under the Apache 2.0 license. The CBOM format is part of [OWASP CycloneDX](https://cyclonedx.org/capabilities/cbom/), published as ECMA-424. [BouncyCastle](https://www.bouncycastle.org/) is by the Legion of the Bouncy Castle, under the [Bouncy Castle Licence](https://www.bouncycastle.org/licence.html). [java-jwt](https://github.com/auth0/java-jwt) is by Auth0, under the MIT license. [jBCrypt](https://www.mindrot.org/projects/jBCrypt/) is by Damien Miller, under the ISC license. None of them is ours. The app, the probe, the two scripts and the tables above are.
