package lab;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.KDF;
import javax.crypto.KeyAgreement;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.HKDFParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The cryptography the JDK ships (JCA), one case per method.
 * A case starts at its "// [ID]" line and ends at the next one.
 * Each method does one complete operation and returns what the JDK did.
 */
public final class Jca {
    private Jca() {}

    static final byte[] DATA = "note 013".getBytes(StandardCharsets.UTF_8);
    private static final String DIGEST = "SHA-512";

    /** Fixed bytes for keys, salts and nonces: the probe measures names, not secrets. */
    static byte[] bytes(int n) {
        byte[] out = new byte[n];
        for (int i = 0; i < n; i++) {
            out[i] = (byte) (i + 1);
        }
        return out;
    }

    // [J01] hash, name in the call
    static String j01() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        return md.getAlgorithm() + ", " + md.digest(DATA).length * 8 + " bits";
    }

    // [J02] hash, name in a constant
    static String j02() throws Exception {
        MessageDigest md = MessageDigest.getInstance(DIGEST);
        return md.getAlgorithm() + ", " + md.digest(DATA).length * 8 + " bits";
    }

    // [J03] hash, name passed through a method
    static String j03() throws Exception {
        MessageDigest md = pick("SHA-384");
        return md.getAlgorithm() + ", " + md.digest(DATA).length * 8 + " bits";
    }

    private static MessageDigest pick(String name) throws Exception {
        return MessageDigest.getInstance(name);
    }

    // [J04] hash, name known only when the app runs
    static String j04(Map<String, String> config) throws Exception {
        MessageDigest md = MessageDigest.getInstance(config.get("digest"));
        return md.getAlgorithm() + ", " + md.digest(DATA).length * 8 + " bits";
    }

    // [J05] hash, SHA-3
    static String j05() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA3-256");
        return md.getAlgorithm() + ", " + md.digest(DATA).length * 8 + " bits";
    }

    // [J06] MAC
    static String j06() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(bytes(32), "HmacSHA256"));
        return mac.getAlgorithm() + ", " + mac.doFinal(DATA).length * 8 + " bits";
    }

    // [J07] key derivation from a password, PBKDF2
    static String j07() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        PBEKeySpec spec = new PBEKeySpec("password".toCharArray(), bytes(16), 100_000, 256);
        byte[] key = factory.generateSecret(spec).getEncoded();
        return factory.getAlgorithm() + ", 100000 iterations, " + key.length * 8 + " bits";
    }

    // [J08] key derivation, HKDF through the KDF API (JDK 25)
    static String j08() throws Exception {
        KDF kdf = KDF.getInstance("HKDF-SHA256");
        SecretKey key = kdf.deriveKey("AES",
                HKDFParameterSpec.ofExtract().addIKM(bytes(32)).addSalt(bytes(16)).thenExpand(DATA, 32));
        return kdf.getAlgorithm() + ", " + key.getEncoded().length * 8 + " bits";
    }

    // [J09] cipher, AES-GCM with a 256-bit key generated here
    static String j09() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        SecretKey key = generator.generateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, bytes(12)));
        return cipher.getAlgorithm() + ", key " + key.getEncoded().length * 8 + " bits, "
                + cipher.doFinal(DATA).length + " bytes out";
    }

    // [J10] cipher, AES-CBC with a 128-bit key built from bytes
    static String j10() throws Exception {
        SecretKey key = new SecretKeySpec(bytes(16), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(bytes(16)));
        return cipher.getAlgorithm() + ", key " + key.getEncoded().length * 8 + " bits, "
                + cipher.doFinal(DATA).length + " bytes out";
    }

    // [J11] cipher, "AES" alone: the JDK fills in ECB and PKCS5Padding
    static String j11() throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(bytes(16), "AES"));
        return cipher.getAlgorithm() + ", IV " + (cipher.getIV() == null ? "none" : "set") + ", "
                + cipher.doFinal(DATA).length + " bytes out";
    }

    // [J12] cipher, AES-GCM with the key size in the name
    static String j12() throws Exception {
        Cipher cipher = Cipher.getInstance("AES_256/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(bytes(32), "AES"), new GCMParameterSpec(128, bytes(12)));
        return cipher.getAlgorithm() + ", " + cipher.doFinal(DATA).length + " bytes out";
    }

    // [J13] cipher, ChaCha20-Poly1305
    static String j13() throws Exception {
        Cipher cipher = Cipher.getInstance("ChaCha20-Poly1305");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(bytes(32), "ChaCha20"), new IvParameterSpec(bytes(12)));
        return cipher.getAlgorithm() + ", " + cipher.doFinal(DATA).length + " bytes out";
    }

    // [J14] cipher, triple DES, a legacy algorithm
    static String j14() throws Exception {
        SecretKey key = KeyGenerator.getInstance("DESede").generateKey();
        Cipher cipher = Cipher.getInstance("DESede/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(bytes(8)));
        return cipher.getAlgorithm() + ", key " + key.getEncoded().length * 8 + " bits, "
                + cipher.doFinal(DATA).length + " bytes out";
    }

    // [J15] cipher, RSA with OAEP padding, 2048-bit key generated here
    static String j15() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair pair = generator.generateKeyPair();
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, pair.getPublic());
        return cipher.getAlgorithm() + ", " + cipher.doFinal(DATA).length * 8 + " bits out";
    }

    // [J16] key wrap, AES-KW
    static String j16() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/KW/NoPadding");
        cipher.init(Cipher.WRAP_MODE, new SecretKeySpec(bytes(32), "AES"));
        byte[] wrapped = cipher.wrap(new SecretKeySpec(bytes(32), "AES"));
        return cipher.getAlgorithm() + ", " + wrapped.length + " bytes out";
    }

    // [J17] signature, RSA PKCS#1 v1.5 with SHA-256
    static String j17() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(generator.generateKeyPair().getPrivate());
        signature.update(DATA);
        return signature.getAlgorithm() + ", " + signature.sign().length * 8 + " bits";
    }

    // [J18] signature, ECDSA on P-256
    static String j18() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(generator.generateKeyPair().getPrivate());
        signature.update(DATA);
        return signature.getAlgorithm() + ", " + signature.sign().length + " bytes";
    }

    // [J19] signature, RSA-PSS with its parameters set in a second call
    static String j19() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        Signature signature = Signature.getInstance("RSASSA-PSS");
        signature.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        signature.initSign(generator.generateKeyPair().getPrivate());
        signature.update(DATA);
        return signature.getAlgorithm() + ", " + signature.sign().length * 8 + " bits";
    }

    // [J20] signature, Ed25519
    static String j20() throws Exception {
        KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Signature signature = Signature.getInstance("Ed25519");
        signature.initSign(pair.getPrivate());
        signature.update(DATA);
        return signature.getAlgorithm() + ", " + signature.sign().length + " bytes";
    }

    // [J21] key pair, RSA 3072
    static String j21() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(3072);
        RSAPublicKey key = (RSAPublicKey) generator.generateKeyPair().getPublic();
        return key.getAlgorithm() + ", " + key.getModulus().bitLength() + " bits";
    }

    // [J22] key pair, elliptic curve P-384
    static String j22() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        PublicKey key = generator.generateKeyPair().getPublic();
        return key.getAlgorithm() + ", " + key.getEncoded().length + " bytes encoded";
    }

    // [J23] key agreement, X25519
    static String j23() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("X25519");
        KeyPair ours = generator.generateKeyPair();
        KeyPair theirs = generator.generateKeyPair();
        KeyAgreement agreement = KeyAgreement.getInstance("X25519");
        agreement.init(ours.getPrivate());
        agreement.doPhase(theirs.getPublic(), true);
        return agreement.getAlgorithm() + ", " + agreement.generateSecret().length * 8 + " bits";
    }

    // [J24] key agreement, finite-field Diffie-Hellman, 2048 bits
    static String j24() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("DH");
        generator.initialize(2048);
        KeyPair ours = generator.generateKeyPair();
        KeyPairGenerator same = KeyPairGenerator.getInstance("DH");
        same.initialize(((DHPublicKey) ours.getPublic()).getParams());
        KeyPair theirs = same.generateKeyPair();
        KeyAgreement agreement = KeyAgreement.getInstance("DH");
        agreement.init(ours.getPrivate());
        agreement.doPhase(theirs.getPublic(), true);
        return agreement.getAlgorithm() + ", " + agreement.generateSecret().length * 8 + " bits";
    }

    // [J25] key import, an encoded public key read back through a KeyFactory
    static String j25() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        byte[] encoded = generator.generateKeyPair().getPublic().getEncoded();
        PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encoded));
        return key.getAlgorithm() + ", " + key.getFormat();
    }

    // [J26] random numbers, generator named in the call
    static String j26() throws Exception {
        SecureRandom random = SecureRandom.getInstance("DRBG");
        return random.getAlgorithm() + ", " + random.generateSeed(16).length + " bytes";
    }

    // [J27] random numbers, the JDK's default generator
    static String j27() {
        SecureRandom random = new SecureRandom();
        byte[] out = new byte[16];
        random.nextBytes(out);
        return random.getAlgorithm() + ", " + out.length + " bytes";
    }
}
