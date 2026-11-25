package lab;

import java.security.AsymmetricKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.spec.NamedParameterSpec;
import javax.crypto.KEM;
import javax.crypto.SecretKey;

/**
 * Post-quantum algorithms and the key encapsulation API, as the JDK ships them.
 * ML-KEM and ML-DSA exist from JDK 24, the KEM API from JDK 21.
 */
public final class Pqc {
    private Pqc() {}

    private static String parameterSet(java.security.Key key) {
        return ((NamedParameterSpec) ((AsymmetricKey) key).getParams()).getName();
    }

    // [P01] key encapsulation, ML-KEM-768 named in the key pair generator
    static String p01() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("ML-KEM-768").generateKeyPair();
        KEM kem = KEM.getInstance("ML-KEM");
        KEM.Encapsulated sent = kem.newEncapsulator(pair.getPublic()).encapsulate();
        SecretKey shared = kem.newDecapsulator(pair.getPrivate()).decapsulate(sent.encapsulation());
        return parameterSet(pair.getPublic()) + ", ciphertext " + sent.encapsulation().length + " bytes, secret "
                + shared.getEncoded().length * 8 + " bits";
    }

    // [P02] key encapsulation, ML-KEM with the parameter set in a second call
    static String p02() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("ML-KEM");
        generator.initialize(NamedParameterSpec.ML_KEM_1024);
        KeyPair pair = generator.generateKeyPair();
        return parameterSet(pair.getPublic()) + ", public key " + pair.getPublic().getEncoded().length + " bytes encoded";
    }

    // [P03] key encapsulation, the same API with a classical algorithm (DHKEM on X25519)
    static String p03() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("X25519").generateKeyPair();
        KEM kem = KEM.getInstance("DHKEM");
        KEM.Encapsulated sent = kem.newEncapsulator(pair.getPublic()).encapsulate();
        SecretKey shared = kem.newDecapsulator(pair.getPrivate()).decapsulate(sent.encapsulation());
        return kem.getAlgorithm() + ", ciphertext " + sent.encapsulation().length + " bytes, secret "
                + shared.getEncoded().length * 8 + " bits";
    }

    // [P04] signature, ML-DSA-65
    static String p04() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("ML-DSA-65").generateKeyPair();
        Signature signature = Signature.getInstance("ML-DSA");
        signature.initSign(pair.getPrivate());
        signature.update(Jca.DATA);
        return parameterSet(pair.getPublic()) + ", signature " + signature.sign().length + " bytes";
    }
}
