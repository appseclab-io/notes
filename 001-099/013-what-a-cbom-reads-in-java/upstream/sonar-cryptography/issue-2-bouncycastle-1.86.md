<!--
Posted on 25 Nov 2026 as https://github.com/cbomkit/sonar-cryptography/issues/573
by the founder, with the line linking note 013 added at the end of Reproduce.
Every "Run" and "CBOM" line below comes from results-run.txt and results-sonar.txt of this folder
(scan of 25 Nov 2026): cases N02-N04 (bc187), M02-M04 and O02-O04 (bc185).
Title and body below the line, as posted.
-->

**Title:** BouncyCastle 1.86 and later: ML-KEM and ML-DSA of the light-weight API are no longer detected (and SLH-DSA in no version)

---

### Versions

sonar-cryptography 1.7.0 on SonarQube Community Build 26.4.0, scanned with sonar-maven-plugin 5.9.0.7382 on Temurin JDK 27. Quality profile: Sonar way plus *Cryptographic Inventory (CBOM)*. BouncyCastle `bcprov-jdk18on` 1.85.2 and 1.87.

### What happens

BouncyCastle 1.86 moved the ML-KEM, ML-DSA and SLH-DSA classes of its light-weight API. Listing the jars:

| `bcprov-jdk18on` | `org.bouncycastle.pqc.crypto.mlkem`, `.mldsa`, `.slhdsa` | `org.bouncycastle.crypto.generators`, `.params`, `.kems`, `.signers` |
| --- | --- | --- |
| 1.84 | present | present |
| 1.85.2 | present | present |
| 1.86 | gone | present |
| 1.87 | gone | present |

We wrote the same three operations (ML-KEM-768 encapsulation, ML-DSA-65 signature, SLH-DSA signature) three times, ran them all, and scanned them as one project. Only the imports differ between the classes (full class below).

| Classes | `bcprov-jdk18on` | ML-KEM-768 | ML-DSA-65 | SLH-DSA |
| --- | --- | --- | --- | --- |
| `org.bouncycastle.pqc.crypto.*` | 1.85.2 | `ML-KEM` on `MLKEMGenerator` and `MLKEMExtractor` | `ML-DSA` on `MLDSASigner` | nothing |
| `org.bouncycastle.crypto.*` | 1.85.2 | nothing | nothing | nothing |
| `org.bouncycastle.crypto.*` | 1.87 | nothing | nothing | nothing |

All nine ran: ciphertext 1088 bytes and a 256-bit secret for ML-KEM, signatures of 3309 bytes for ML-DSA and 17088 bytes for SLH-DSA.

### Where it seems to come from

At tag 1.7.0 the rules name the old location: `BcMLDSASigner` matches `org.bouncycastle.pqc.crypto.mldsa.MLDSASigner`, `BcMLKEMKeyParameters` matches `org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyParameters`, and `BcEncapsulatedSecretGenerator` maps `MLKEMGenerator` to `org.bouncycastle.pqc.crypto.mlkem.`. The plugin's test dependency is `bcprov-jdk18on` 1.85.2, the last version where that location exists.

For SLH-DSA, `BcMessageSigner` knows `SPHINCSPlusSigner`, the class from before FIPS 205, and no Java rule names `SLHDSASigner`.

### Why it matters

A project that upgrades BouncyCastle from 1.85 to 1.86 changes its imports, keeps compiling, and its CBOM loses its post-quantum entries with no warning. An inventory kept to follow a post-quantum migration then shows less of it after the upgrade.

### What would help

1. Match both locations in the ML-KEM and ML-DSA rules, and move the test dependency to a current `bcprov-jdk18on`.
2. A rule for `SLHDSASigner` (and `HashSLHDSASigner`, which exists next to it).

The entries found on 1.85.2 carry no parameter set (`ML-KEM`, `ML-DSA`): that is #149, and the comment of 10 Nov 2026 there shows the same for ML-KEM on 1.80.

### Reproduce

Two Maven modules, one on `bcprov-jdk18on` 1.87 and one on 1.85.2, compiled with `--release 25`, scanned as one project with `mvn compile org.sonarsource.scanner.maven:sonar-maven-plugin:5.9.0.7382:sonar`. The class with the old imports is below. For the new location, replace its imports with these and keep the rest:

```java
import org.bouncycastle.crypto.generators.MLDSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.MLKEMKeyPairGenerator;
import org.bouncycastle.crypto.generators.SLHDSAKeyPairGenerator;
import org.bouncycastle.crypto.kems.MLKEMExtractor;
import org.bouncycastle.crypto.kems.MLKEMGenerator;
import org.bouncycastle.crypto.params.MLDSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.MLDSAParameters;
import org.bouncycastle.crypto.params.MLKEMKeyGenerationParameters;
import org.bouncycastle.crypto.params.MLKEMParameters;
import org.bouncycastle.crypto.params.MLKEMPrivateKeyParameters;
import org.bouncycastle.crypto.params.SLHDSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.SLHDSAParameters;
import org.bouncycastle.crypto.signers.MLDSASigner;
import org.bouncycastle.crypto.signers.SLHDSASigner;
```

<details>
<summary>Pq185Old.java</summary>

```java
package lab;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMExtractor;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMParameters;
import org.bouncycastle.pqc.crypto.mlkem.MLKEMPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSAKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSAKeyPairGenerator;
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSAParameters;
import org.bouncycastle.pqc.crypto.slhdsa.SLHDSASigner;

public final class Pq185Old {
    private Pq185Old() {}

    private static final byte[] DATA = "note 013".getBytes(StandardCharsets.UTF_8);

    // [O02] key encapsulation, ML-KEM-768
    static String kem() {
        MLKEMKeyPairGenerator generator = new MLKEMKeyPairGenerator();
        generator.init(new MLKEMKeyGenerationParameters(new SecureRandom(), MLKEMParameters.ml_kem_768));
        AsymmetricCipherKeyPair pair = generator.generateKeyPair();
        SecretWithEncapsulation sent = new MLKEMGenerator(new SecureRandom()).generateEncapsulated(pair.getPublic());
        byte[] secret = new MLKEMExtractor((MLKEMPrivateKeyParameters) pair.getPrivate())
                .extractSecret(sent.getEncapsulation());
        return MLKEMParameters.ml_kem_768.getName() + ", ciphertext " + sent.getEncapsulation().length
                + " bytes, secret " + secret.length * 8 + " bits";
    }

    // [O03] signature, ML-DSA-65
    static String dsa() throws Exception {
        MLDSAKeyPairGenerator generator = new MLDSAKeyPairGenerator();
        generator.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
        MLDSASigner signer = new MLDSASigner();
        signer.init(true, generator.generateKeyPair().getPrivate());
        signer.update(DATA, 0, DATA.length);
        return MLDSAParameters.ml_dsa_65.getName() + ", signature " + signer.generateSignature().length + " bytes";
    }

    // [O04] signature, SLH-DSA
    static String slh() throws Exception {
        SLHDSAKeyPairGenerator generator = new SLHDSAKeyPairGenerator();
        generator.init(new SLHDSAKeyGenerationParameters(new SecureRandom(), SLHDSAParameters.sha2_128f));
        SLHDSASigner signer = new SLHDSASigner();
        signer.init(true, generator.generateKeyPair().getPrivate());
        return SLHDSAParameters.sha2_128f.getName() + ", signature " + signer.generateSignature(DATA).length + " bytes";
    }
}
```

</details>

The full test, with 55 cases read by this plugin and by cdxgen, is written up at https://www.appseclab.io/blog/013-what-a-cbom-reads-in-java/

### AI use

The test cases, the reading of the plugin's sources and this text were written with an AI coding assistant (Claude) and reviewed by me. Every run and every scan quoted here was executed on my machine and server.
