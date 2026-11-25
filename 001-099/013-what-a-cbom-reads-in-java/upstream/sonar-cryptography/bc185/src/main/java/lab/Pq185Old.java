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

/** BouncyCastle 1.85.2: the same three operations through org.bouncycastle.pqc.crypto, the package 1.86 removed. */
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
