# Upstream: sonar-cryptography

What we found about the SonarQube cryptography plugin in note 013, reduced to the smallest code that shows it, and the three reports written from it for the project, [cbomkit/sonar-cryptography](https://github.com/cbomkit/sonar-cryptography).

## What is in the folder

| File | What it is |
| --- | --- |
| `jdk/src/main/java/lab/Sizes.java` | S01 to S07: the code fixes a key size or a parameter set |
| `jdk/src/main/java/lab/Coverage.java` | C01 to C05: `KEM`, `KDF`, `SecureRandom` and TLS parameters of the JDK |
| `jdk/src/main/java/lab/Inputs.java` | What the caller passes to S03 and C01. It is not a case |
| `bc187/src/main/java/lab/` | BouncyCastle 1.87: N01 AES-GCM, N02 to N04 ML-KEM, ML-DSA and SLH-DSA where 1.86 and later keep them |
| `bc185/src/main/java/lab/` | BouncyCastle 1.85.2: M02 to M04, the same three in the new location, and O02 to O04 in the old one |
| `pom.xml` and one `pom.xml` per module | The three modules, built and scanned together |
| `results-run.txt` | Our run: what each case did |
| `scan.sh` | Scans the three modules as one project |
| `cbom-sonar-repro.json` | The CBOM the plugin wrote |
| `results-sonar.txt` | The plugin's count and, case by case, its entries, from `../../cases.mjs` |
| `issue-1-sizes.md`, `issue-2-bouncycastle-1.86.md`, `issue-3-jdk-apis.md` | The three reports, each with its title and the body to paste |

## Run it

Each module runs on its own, from this folder:

```bash
mvn -q compile exec:java -pl jdk
```

The same with `bc187` and `bc185`. The scan needs a SonarQube with the plugin, a project `notes-java-cbom-repro` on a Java quality profile with the rule *Cryptographic Inventory (CBOM)*, and a token:

```bash
SONAR_HOST_URL=https://sonar.example.com ./scan.sh
```

The plugin writes `cbom.json` in this folder; the file kept here is that one, renamed. Then:

```bash
node ../../cases.mjs cbom-sonar-repro.json .
```

## Reading the output

- **Sizes.** S01, S03, S04, S05, S06, S07 and N01 have a size or a parameter set the run contradicts. S02, the same cipher with a key built from `new byte[32]`, is read right.
- **BouncyCastle.** On 1.85.2 the classes of `org.bouncycastle.pqc.crypto` give `ML-KEM` and `ML-DSA` entries, with no parameter set; the same classes of `org.bouncycastle.crypto` give nothing, on 1.85.2 and on 1.87. SLH-DSA gives nothing in either.
- **JDK APIs.** C01 to C05 give nothing. The ML-KEM key pair made in `Inputs.java` is listed.

## Status

Posted on 25 November 2026 from the founder's GitHub account:

- [issue 572](https://github.com/cbomkit/sonar-cryptography/issues/572): key sizes and parameter sets (`issue-1-sizes.md`)
- [issue 573](https://github.com/cbomkit/sonar-cryptography/issues/573): BouncyCastle 1.86 and SLH-DSA (`issue-2-bouncycastle-1.86.md`)
- [issue 574](https://github.com/cbomkit/sonar-cryptography/issues/574): `KEM`, `KDF`, `SecureRandom` and TLS named groups (`issue-3-jdk-apis.md`)

Each file holds the text as posted. Follow the issues to see whether a later version of the plugin changes these results.
