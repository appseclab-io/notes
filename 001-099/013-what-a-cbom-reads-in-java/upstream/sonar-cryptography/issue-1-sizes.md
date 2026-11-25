<!--
Posted on 25 Nov 2026 as https://github.com/cbomkit/sonar-cryptography/issues/572
by the founder, with the line linking note 013 added at the end of Reproduce.
Every "Run" and "CBOM" line below comes from results-run.txt and results-sonar.txt of this folder
(scan of 25 Nov 2026), except the last paragraph of "What happens", which is case J12 of the lab's
55-case probe (results-sonar.txt one folder up).
Title and body below the line, as posted.
-->

**Title:** CBOM states a key size or parameter set the code does not use (AES 128, ML-KEM-768, ML-DSA-65, FFDH-3072, 3DES 64)

---

### Versions

sonar-cryptography 1.7.0 on SonarQube Community Build 26.4.0, scanned with sonar-maven-plugin 5.9.0.7382 on Temurin JDK 27. Quality profile: Sonar way plus *Cryptographic Inventory (CBOM)*.

### What happens

Each snippet is part of a method that runs (full class below). In each one the CBOM carries a key size or a parameter set that is not the one the code uses, and nothing in the entry tells it apart from a value read from the code.

| Case | Code | Run | CBOM |
| --- | --- | --- | --- |
| S01 | `KeyGenerator` for AES, `init(256)`, the key given to `Cipher.getInstance("AES/GCM/NoPadding")` two lines below | key 256 bits | key generator `AES-256`; cipher `AES-128-GCM`, `parameterSetIdentifier: "128"` |
| S03 | `Cipher.getInstance("AES/GCM/NoPadding")` in a method that receives the key as a parameter | called with a 256-bit key | `AES-128-GCM`, `"128"` |
| S04 | `KeyPairGenerator.getInstance("ML-KEM")`, then `initialize(NamedParameterSpec.ML_KEM_1024)` | `ML-KEM-1024` | `ML-KEM-768`, `"768"` |
| S05 | `KeyPairGenerator.getInstance("ML-DSA")`, then `initialize(NamedParameterSpec.ML_DSA_87)` | `ML-DSA-87`, signature 4627 bytes | `ML-DSA-65` on the generator and on `Signature.getInstance("ML-DSA")` |
| S06 | `KeyPairGenerator.getInstance("DH")`, `initialize(2048)`, two key pairs, `KeyAgreement.getInstance("DH")` | 2048 bits | generator `FFDH-2048`; agreement `FFDH-3072` |
| S07 | `KeyGenerator.getInstance("DESede")`, `Cipher.getInstance("DESede/CBC/PKCS5Padding")` | key 192 bits | `3DES` and `3DES-CBC`, `"64"` |
| N01 | BouncyCastle 1.87: `GCMBlockCipher.newInstance(AESEngine.newInstance())` with `new KeyParameter(new byte[32])` | key 256 bits | `AES-GCM` and `AES`, `"128"` |

For contrast, S02 is read right: `new SecretKeySpec(new byte[32], "AES")` given to the same cipher gives `AES-256` and `AES-256-GCM`, thanks to the fix of #294. In a larger test of ours the same key built from an array a method returns, `new SecretKeySpec(bytes(32), "AES")`, came out as `AES-128`.

### Where it seems to come from

Reading the sources at tag 1.7.0:

- `AESEnricher` calls `applyDefaultKeySizeForJca(aes, 128)` (`IEnrichWithDefaultKeySize`): an AES node of the Jca bundle with no `KeyLength` gets `KeyLength(128, …, NodeOrigin.ENRICHED)`. `DHEnricher` does the same with 3072. The origin stays in the model; the CBOM does not carry it.
- `JcaKemMapper` maps the name `"ML-KEM"` to `new MLKEM(768, …)`, and `JcaSignatureMapper` maps `"ML-DSA"` to 65. No rule under `jca/algorithmspec` reads `NamedParameterSpec`.
- `CBOMOutputFile` fills `parameterSetIdentifier` with `Utils.oneOf(KeyLength, DigestSize, BlockSize, ParameterSetIdentifier)`. With no key length the block size is written: 64 for DESede, which `DESEnricher` does not cover, and 128 for the AES of `BcBlockCipherEngineMapper`, which is built without a key length.
- #294 was closed by 3c6252e ("propagate KeyLength"), which covers a key built with `SecretKeySpec` from an array whose length is in the code. A key from a `KeyGenerator` is not propagated to the cipher that uses it.

Two of these values are the JDK's own defaults: on JDK 27 a `KeyPairGenerator` for `DH` with no size gives 3072 bits, and one for `ML-KEM` gives ML-KEM-768. The AES one is not: a `KeyGenerator` for `AES` with no size gives 256 bits there. A `Cipher` has no default key size at all; the key decides.

### Why it matters

Key size and parameter set are what a migration policy sorts by. A check for "AES-256 only" or "ML-KEM-1024 only" reads these fields: S01, S03, S04, S05 and N01 give it a false finding, and S06 shows a 2048-bit exchange as 3072. A reader of the file cannot tell which values were read from the code.

### What would help

1. Mark a value that was not read from the code. The model already knows (`NodeOrigin.ENRICHED`); a property on the component would let a consumer filter on it.
2. Or leave the field out when the size is unknown, at least for a `Cipher`, where the JDK has no default.
3. Keep `BlockSize` out of `parameterSetIdentifier`.
4. Read `NamedParameterSpec` in `KeyPairGenerator.initialize(...)` for ML-KEM and ML-DSA.
5. Propagate the key size of a `KeyGenerator` to the cipher that uses the key, as 3c6252e does for `SecretKeySpec`.

### Reproduce

A Maven project compiled with `--release 25`, scanned with `mvn compile org.sonarsource.scanner.maven:sonar-maven-plugin:5.9.0.7382:sonar` against a server with the plugin and the rule active. The JDK cases are this class; N01 is the BouncyCastle snippet in the table, with `bcprov-jdk18on` 1.87.

<details>
<summary>Sizes.java</summary>

```java
package lab;

import java.nio.charset.StandardCharsets;
import java.security.AsymmetricKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.NamedParameterSpec;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public final class Sizes {
    private Sizes() {}

    static final byte[] DATA = "note 013".getBytes(StandardCharsets.UTF_8);

    // [S01] AES-GCM, 256-bit key from a KeyGenerator
    static String s01() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, new byte[12]));
        cipher.doFinal(DATA);
        return cipher.getAlgorithm() + ", key " + key.getEncoded().length * 8 + " bits";
    }

    // [S02] AES-GCM, 256-bit key built from an array of 32 bytes
    static String s02() throws Exception {
        SecretKey key = new SecretKeySpec(new byte[32], "AES");
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, new byte[12]));
        cipher.doFinal(DATA);
        return cipher.getAlgorithm() + ", key " + key.getEncoded().length * 8 + " bits";
    }

    // [S03] AES-GCM, the key is a parameter of the method
    static String s03(SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, new byte[12]));
        cipher.doFinal(DATA);
        return cipher.getAlgorithm() + ", key " + key.getEncoded().length * 8 + " bits";
    }

    // [S04] ML-KEM, parameter set ML-KEM-1024 given to initialize()
    static String s04() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ML-KEM");
        generator.initialize(NamedParameterSpec.ML_KEM_1024);
        KeyPair pair = generator.generateKeyPair();
        return ((NamedParameterSpec) ((AsymmetricKey) pair.getPublic()).getParams()).getName();
    }

    // [S05] ML-DSA, parameter set ML-DSA-87 given to initialize()
    static String s05() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ML-DSA");
        generator.initialize(NamedParameterSpec.ML_DSA_87);
        KeyPair pair = generator.generateKeyPair();
        Signature signature = Signature.getInstance("ML-DSA");
        signature.initSign(pair.getPrivate());
        signature.update(DATA);
        return ((NamedParameterSpec) ((AsymmetricKey) pair.getPublic()).getParams()).getName()
                + ", signature " + signature.sign().length + " bytes";
    }

    // [S06] Diffie-Hellman, 2048 bits on both sides
    static String s06() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("DH");
        generator.initialize(2048);
        KeyPair ours = generator.generateKeyPair();
        KeyPair theirs = generator.generateKeyPair();
        KeyAgreement agreement = KeyAgreement.getInstance("DH");
        agreement.init(ours.getPrivate());
        agreement.doPhase(theirs.getPublic(), true);
        return agreement.getAlgorithm() + ", " + ((DHPublicKey) ours.getPublic()).getParams().getP().bitLength()
                + " bits, secret " + agreement.generateSecret().length * 8 + " bits";
    }

    // [S07] triple DES, the key the JDK generates has 192 bits
    static String s07() throws Exception {
        SecretKey key = KeyGenerator.getInstance("DESede").generateKey();
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[8]));
        cipher.doFinal(DATA);
        return cipher.getAlgorithm() + ", key " + key.getEncoded().length * 8 + " bits";
    }
}
```

</details>

The full test, with 55 cases read by this plugin and by cdxgen, is written up at https://www.appseclab.io/blog/013-what-a-cbom-reads-in-java/

### AI use

The test cases, the reading of the plugin's sources and this text were written with an AI coding assistant (Claude) and reviewed by me. Every run and every scan quoted here was executed on my machine and server.
