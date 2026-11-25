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

/** JDK calls that name cryptography through newer or less common APIs. Each method runs. */
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
