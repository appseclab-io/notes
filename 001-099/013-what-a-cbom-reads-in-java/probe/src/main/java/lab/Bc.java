package lab;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
import org.bouncycastle.crypto.agreement.X25519Agreement;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator;
import org.bouncycastle.crypto.generators.MLDSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.MLKEMKeyPairGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.SCrypt;
import org.bouncycastle.crypto.generators.SLHDSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator;
import org.bouncycastle.crypto.kems.MLKEMExtractor;
import org.bouncycastle.crypto.kems.MLKEMGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Argon2Parameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.MLDSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.MLDSAParameters;
import org.bouncycastle.crypto.params.MLKEMKeyGenerationParameters;
import org.bouncycastle.crypto.params.MLKEMParameters;
import org.bouncycastle.crypto.params.MLKEMPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.SLHDSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.SLHDSAParameters;
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters;
import org.bouncycastle.crypto.signers.DSADigestSigner;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.crypto.signers.MLDSASigner;
import org.bouncycastle.crypto.signers.SLHDSASigner;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * BouncyCastle 1.87. B01 to B17 use its light-weight API, where the algorithm
 * is a class and not a name in a string. B18 and B19 go through the JCA with
 * BouncyCastle as the provider. The ML-KEM, ML-DSA and SLH-DSA classes are
 * where 1.86 and later keep them (org.bouncycastle.crypto); up to 1.85 they
 * also lived under org.bouncycastle.pqc.crypto.
 */
public final class Bc {
    private Bc() {}

    private static final byte[] DATA = Jca.DATA;
    private static final byte[] PASSWORD = "password".getBytes(StandardCharsets.UTF_8);

    private static byte[] bytes(int n) {
        return Jca.bytes(n);
    }

    // [B01] hash, SHA-256
    static String b01() {
        SHA256Digest digest = new SHA256Digest();
        digest.update(DATA, 0, DATA.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return digest.getAlgorithmName() + ", " + out.length * 8 + " bits";
    }

    // [B02] hash, SHA-3 with the size as a number
    static String b02() {
        SHA3Digest digest = new SHA3Digest(256);
        digest.update(DATA, 0, DATA.length);
        byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return digest.getAlgorithmName() + ", " + out.length * 8 + " bits";
    }

    // [B03] MAC, HMAC over SHA-256
    static String b03() {
        HMac mac = new HMac(new SHA256Digest());
        mac.init(new KeyParameter(bytes(32)));
        mac.update(DATA, 0, DATA.length);
        byte[] out = new byte[mac.getMacSize()];
        mac.doFinal(out, 0);
        return mac.getAlgorithmName() + ", " + out.length * 8 + " bits";
    }

    // [B04] key derivation from a password, PBKDF2
    static String b04() {
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
        generator.init(PASSWORD, bytes(16), 100_000);
        KeyParameter key = (KeyParameter) generator.generateDerivedParameters(256);
        return "PBKDF2 with HMAC-SHA-256, 100000 iterations, " + key.getKeyLength() * 8 + " bits";
    }

    // [B05] key derivation from a password, scrypt
    static String b05() {
        byte[] key = SCrypt.generate(PASSWORD, bytes(16), 16384, 8, 1, 32);
        return "scrypt N=16384 r=8 p=1, " + key.length * 8 + " bits";
    }

    // [B06] password hashing, Argon2id
    static String b06() {
        Argon2Parameters parameters = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(bytes(16)).withMemoryAsKB(19456).withIterations(2).withParallelism(1).build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(parameters);
        byte[] out = new byte[32];
        generator.generateBytes("password".toCharArray(), out);
        return "Argon2id m=19456 KiB t=2 p=1, " + out.length * 8 + " bits";
    }

    // [B07] cipher, AES-GCM through the factory methods BouncyCastle recommends
    static String b07() throws Exception {
        GCMModeCipher cipher = GCMBlockCipher.newInstance(AESEngine.newInstance());
        cipher.init(true, new AEADParameters(new KeyParameter(bytes(32)), 128, bytes(12)));
        byte[] out = new byte[cipher.getOutputSize(DATA.length)];
        int written = cipher.processBytes(DATA, 0, DATA.length, out, 0);
        cipher.doFinal(out, written);
        return cipher.getAlgorithmName() + ", key 256 bits, " + out.length + " bytes out";
    }

    // [B08] cipher, AES-GCM through the constructors of older code
    @SuppressWarnings("deprecation")
    static String b08() throws Exception {
        GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        cipher.init(true, new AEADParameters(new KeyParameter(bytes(32)), 128, bytes(12)));
        byte[] out = new byte[cipher.getOutputSize(DATA.length)];
        int written = cipher.processBytes(DATA, 0, DATA.length, out, 0);
        cipher.doFinal(out, written);
        return cipher.getAlgorithmName() + ", key 256 bits, " + out.length + " bytes out";
    }

    // [B09] cipher, ChaCha20-Poly1305
    static String b09() throws Exception {
        ChaCha20Poly1305 cipher = new ChaCha20Poly1305();
        cipher.init(true, new AEADParameters(new KeyParameter(bytes(32)), 128, bytes(12)));
        byte[] out = new byte[cipher.getOutputSize(DATA.length)];
        int written = cipher.processBytes(DATA, 0, DATA.length, out, 0);
        cipher.doFinal(out, written);
        return cipher.getAlgorithmName() + ", " + out.length + " bytes out";
    }

    // [B10] cipher, AES-CBC with PKCS#7 padding
    static String b10() throws Exception {
        PaddedBufferedBlockCipher cipher =
                new PaddedBufferedBlockCipher(CBCBlockCipher.newInstance(AESEngine.newInstance()), new PKCS7Padding());
        cipher.init(true, new ParametersWithIV(new KeyParameter(bytes(16)), bytes(16)));
        byte[] out = new byte[cipher.getOutputSize(DATA.length)];
        int written = cipher.processBytes(DATA, 0, DATA.length, out, 0);
        written += cipher.doFinal(out, written);
        return cipher.getUnderlyingCipher().getAlgorithmName() + ", key 128 bits, " + written + " bytes out";
    }

    // [B11] cipher, RSA with OAEP padding, 2048-bit key generated here
    static String b11() throws Exception {
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        generator.init(new RSAKeyGenerationParameters(BigInteger.valueOf(65537), new SecureRandom(), 2048, 100));
        AsymmetricCipherKeyPair pair = generator.generateKeyPair();
        OAEPEncoding cipher = new OAEPEncoding(new RSAEngine(), new SHA256Digest());
        cipher.init(true, pair.getPublic());
        return "RSA-OAEP with SHA-256, " + cipher.processBlock(DATA, 0, DATA.length).length * 8 + " bits out";
    }

    // [B12] signature, ECDSA on P-256 with SHA-256
    static String b12() {
        X9ECParameters curve = ECNamedCurveTable.getByName("secp256r1");
        ECDomainParameters domain = new ECDomainParameters(curve.getCurve(), curve.getG(), curve.getN(), curve.getH());
        ECKeyPairGenerator generator = new ECKeyPairGenerator();
        generator.init(new ECKeyGenerationParameters(domain, new SecureRandom()));
        DSADigestSigner signer = new DSADigestSigner(new ECDSASigner(), new SHA256Digest());
        signer.init(true, generator.generateKeyPair().getPrivate());
        signer.update(DATA, 0, DATA.length);
        return "ECDSA on secp256r1 with SHA-256, " + signer.generateSignature().length + " bytes";
    }

    // [B13] signature, Ed25519
    static String b13() {
        Ed25519KeyPairGenerator generator = new Ed25519KeyPairGenerator();
        generator.init(new Ed25519KeyGenerationParameters(new SecureRandom()));
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, generator.generateKeyPair().getPrivate());
        signer.update(DATA, 0, DATA.length);
        return "Ed25519, " + signer.generateSignature().length + " bytes";
    }

    // [B14] key agreement, X25519
    static String b14() {
        X25519KeyPairGenerator generator = new X25519KeyPairGenerator();
        generator.init(new X25519KeyGenerationParameters(new SecureRandom()));
        AsymmetricCipherKeyPair ours = generator.generateKeyPair();
        AsymmetricCipherKeyPair theirs = generator.generateKeyPair();
        X25519Agreement agreement = new X25519Agreement();
        agreement.init(ours.getPrivate());
        byte[] secret = new byte[agreement.getAgreementSize()];
        agreement.calculateAgreement(theirs.getPublic(), secret, 0);
        return "X25519, " + secret.length * 8 + " bits";
    }

    // [B15] key encapsulation, ML-KEM-768
    static String b15() {
        MLKEMKeyPairGenerator generator = new MLKEMKeyPairGenerator();
        generator.init(new MLKEMKeyGenerationParameters(new SecureRandom(), MLKEMParameters.ml_kem_768));
        AsymmetricCipherKeyPair pair = generator.generateKeyPair();
        SecretWithEncapsulation sent = new MLKEMGenerator(new SecureRandom()).generateEncapsulated(pair.getPublic());
        byte[] secret = new MLKEMExtractor((MLKEMPrivateKeyParameters) pair.getPrivate())
                .extractSecret(sent.getEncapsulation());
        return MLKEMParameters.ml_kem_768.getName() + ", ciphertext " + sent.getEncapsulation().length
                + " bytes, secret " + secret.length * 8 + " bits";
    }

    // [B16] signature, ML-DSA-65
    static String b16() throws Exception {
        MLDSAKeyPairGenerator generator = new MLDSAKeyPairGenerator();
        generator.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
        MLDSASigner signer = new MLDSASigner();
        signer.init(true, generator.generateKeyPair().getPrivate());
        signer.update(DATA, 0, DATA.length);
        return MLDSAParameters.ml_dsa_65.getName() + ", signature " + signer.generateSignature().length + " bytes";
    }

    // [B17] signature, SLH-DSA
    static String b17() throws Exception {
        SLHDSAKeyPairGenerator generator = new SLHDSAKeyPairGenerator();
        generator.init(new SLHDSAKeyGenerationParameters(new SecureRandom(), SLHDSAParameters.sha2_128f));
        SLHDSASigner signer = new SLHDSASigner();
        signer.init(true, generator.generateKeyPair().getPrivate());
        return SLHDSAParameters.sha2_128f.getName() + ", signature " + signer.generateSignature(DATA).length + " bytes";
    }

    // [B18] signature, ML-DSA-65 through the JCA with BouncyCastle as the provider
    static String b18() throws Exception {
        Provider provider = new BouncyCastleProvider();
        KeyPair pair = KeyPairGenerator.getInstance("ML-DSA-65", provider).generateKeyPair();
        Signature signature = Signature.getInstance("ML-DSA", provider);
        signature.initSign(pair.getPrivate());
        signature.update(DATA);
        return pair.getPublic().getAlgorithm() + " from " + signature.getProvider().getName() + ", signature "
                + signature.sign().length + " bytes";
    }

    // [B19] cipher, AES-GCM through the JCA with BouncyCastle as the provider
    static String b19() throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", new BouncyCastleProvider());
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(bytes(32), "AES"), new GCMParameterSpec(128, bytes(12)));
        return cipher.getAlgorithm() + " from " + cipher.getProvider().getName() + ", "
                + cipher.doFinal(DATA).length + " bytes out";
    }
}
