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

/**
 * Cases where the code fixes a key size or a parameter set. Each method runs
 * and returns what the JDK did. Keys and nonces of zero bytes are there so a
 * case names an algorithm and nothing else.
 */
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
