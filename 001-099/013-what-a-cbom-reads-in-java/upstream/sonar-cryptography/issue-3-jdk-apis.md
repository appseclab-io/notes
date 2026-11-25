<!--
Posted on 25 Nov 2026 as https://github.com/cbomkit/sonar-cryptography/issues/574
by the founder, with the line linking note 013 added at the end of Reproduce.
Every "Run" line and every "no entry" below comes from results-run.txt and results-sonar.txt of
this folder (scan of 25 Nov 2026): cases C01-C05, and Inputs.java for the listed ML-KEM key pair.
Title and body below the line, as posted.
-->

**Title:** Java: `javax.crypto.KEM`, `javax.crypto.KDF`, `SecureRandom` and `SSLParameters.setNamedGroups` produce no entry

---

### Versions

sonar-cryptography 1.7.0 on SonarQube Community Build 26.4.0, scanned with sonar-maven-plugin 5.9.0.7382 on Temurin JDK 27. Quality profile: Sonar way plus *Cryptographic Inventory (CBOM)*.

### What happens

Each call below runs on JDK 27 and names an algorithm in a string. None gets an entry in the CBOM (full class below).

| Case | Call | Run |
| --- | --- | --- |
| C01 | `KEM.getInstance("ML-KEM")`, then `newEncapsulator(...).encapsulate()` | `ML-KEM, ciphertext 1088 bytes, secret 256 bits` |
| C02 | `KDF.getInstance("HKDF-SHA256")`, then `deriveKey(...)` | `HKDF-SHA256, 256 bits` |
| C03 | `SecureRandom.getInstance("DRBG")` | `DRBG, 16 bytes` |
| C04 | `SSLParameters.setNamedGroups(new String[] {"X25519MLKEM768"})`, applied with `SSLEngine.setSSLParameters(...)` | `named groups X25519MLKEM768` |
| C05 | `SSLParameters.setProtocols(...)` and `setCipherSuites(...)`, applied with `SSLEngine.setSSLParameters(...)` | `TLSv1.2, TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384` |

The key pair that C01 receives is made by `KeyPairGenerator.getInstance("ML-KEM-768")` in another class, and that line is listed as `ML-KEM-768`: the key generation is in the CBOM, the encapsulation is not.

### What we found in the sources

At tag 1.7.0:

- No rule under `jca/` names `javax.crypto.KEM` or `javax.crypto.KDF`.
- `random/SecureRandomGetInstance.java` exists, and `JavaDetectionRules.buildRules()` builds the Java rule set from `JcaDetectionRules`, `BouncyCastleDetectionRules` and `SSLDetectionRules` only.
- `SSLDetectionRules` holds `SSLServerSocketSetEnabledProtocols`, `SSLSetParameters`, `SSLContext` and `SSLServerSocketSetEnabledCipherSuites`. `SSLSetParameters` matches `setSSLParameters` on `javax.net.ssl.SSLServerSocket`; the same parameters applied to an `SSLEngine` are not read. No rule reads `setNamedGroups`.

### Why it matters

`KEM` is the API the JDK gives for ML-KEM, and `setNamedGroups` is where an application asks for a hybrid post-quantum key exchange in TLS. For an inventory kept to follow a post-quantum migration, these are the calls it is looking for.

### What would help

Rules for `KEM.getInstance`, `KDF.getInstance` and `SSLParameters.setNamedGroups`; `SSLSetParameters` on `SSLEngine`; and a word on whether the `SecureRandom` rule is left out on purpose.

### Reproduce

A Maven project compiled with `--release 25`, scanned with `mvn compile org.sonarsource.scanner.maven:sonar-maven-plugin:5.9.0.7382:sonar`. C01 is called with `KeyPairGenerator.getInstance("ML-KEM-768").generateKeyPair()`.

<details>
<summary>Coverage.java</summary>

```java
package lab;

import java.security.KeyPair;
import java.security.SecureRandom;
import javax.crypto.KDF;
import javax.crypto.KEM;
import javax.crypto.SecretKey;
import javax.crypto.spec.HKDFParameterSpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;

public final class Coverage {
    private Coverage() {}

    // [C01] key encapsulation through javax.crypto.KEM, the key pair comes from the caller
    static String c01(KeyPair pair) throws Exception {
        KEM kem = KEM.getInstance("ML-KEM");
        KEM.Encapsulated sent = kem.newEncapsulator(pair.getPublic()).encapsulate();
        SecretKey shared = kem.newDecapsulator(pair.getPrivate()).decapsulate(sent.encapsulation());
        return kem.getAlgorithm() + ", ciphertext " + sent.encapsulation().length + " bytes, secret "
                + shared.getEncoded().length * 8 + " bits";
    }

    // [C02] key derivation through javax.crypto.KDF
    static String c02() throws Exception {
        KDF kdf = KDF.getInstance("HKDF-SHA256");
        SecretKey key = kdf.deriveKey("AES",
                HKDFParameterSpec.ofExtract().addIKM(new byte[32]).addSalt(new byte[16]).thenExpand(Sizes.DATA, 32));
        return kdf.getAlgorithm() + ", " + key.getEncoded().length * 8 + " bits";
    }

    // [C03] random numbers, generator named in the call
    static String c03() throws Exception {
        SecureRandom random = SecureRandom.getInstance("DRBG");
        return random.getAlgorithm() + ", " + random.generateSeed(16).length + " bytes";
    }

    // [C04] TLS, key exchange group set by name
    static String c04() throws Exception {
        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
        SSLParameters parameters = engine.getSSLParameters();
        parameters.setNamedGroups(new String[] {"X25519MLKEM768"});
        engine.setSSLParameters(parameters);
        return "named groups " + String.join(",", engine.getSSLParameters().getNamedGroups());
    }

    // [C05] TLS, protocol and cipher suite set on the parameters of an engine
    static String c05() throws Exception {
        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
        SSLParameters parameters = engine.getSSLParameters();
        parameters.setProtocols(new String[] {"TLSv1.2"});
        parameters.setCipherSuites(new String[] {"TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"});
        engine.setSSLParameters(parameters);
        return String.join(",", engine.getEnabledProtocols()) + ", "
                + String.join(",", engine.getEnabledCipherSuites());
    }
}
```

`Sizes.DATA` is `"note 013".getBytes(StandardCharsets.UTF_8)`.

</details>

The full test, with 55 cases read by this plugin and by cdxgen, is written up at https://www.appseclab.io/blog/013-what-a-cbom-reads-in-java/

### AI use

The test cases, the reading of the plugin's sources and this text were written with an AI coding assistant (Claude) and reviewed by me. Every run and every scan quoted here was executed on my machine and server.
