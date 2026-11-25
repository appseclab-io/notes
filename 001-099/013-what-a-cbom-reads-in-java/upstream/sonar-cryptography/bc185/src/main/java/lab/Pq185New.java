package lab;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.SecretWithEncapsulation;
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

/** BouncyCastle 1.85.2: the same three operations through org.bouncycastle.crypto. */
public final class Pq185New {
    private Pq185New() {}

    private static final byte[] DATA = "note 013".getBytes(StandardCharsets.UTF_8);

    // [M02] key encapsulation, ML-KEM-768
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

    // [M03] signature, ML-DSA-65
    static String dsa() throws Exception {
        MLDSAKeyPairGenerator generator = new MLDSAKeyPairGenerator();
        generator.init(new MLDSAKeyGenerationParameters(new SecureRandom(), MLDSAParameters.ml_dsa_65));
        MLDSASigner signer = new MLDSASigner();
        signer.init(true, generator.generateKeyPair().getPrivate());
        signer.update(DATA, 0, DATA.length);
        return MLDSAParameters.ml_dsa_65.getName() + ", signature " + signer.generateSignature().length + " bytes";
    }

    // [M04] signature, SLH-DSA
    static String slh() throws Exception {
        SLHDSAKeyPairGenerator generator = new SLHDSAKeyPairGenerator();
        generator.init(new SLHDSAKeyGenerationParameters(new SecureRandom(), SLHDSAParameters.sha2_128f));
        SLHDSASigner signer = new SLHDSASigner();
        signer.init(true, generator.generateKeyPair().getPrivate());
        return SLHDSAParameters.sha2_128f.getName() + ", signature " + signer.generateSignature(DATA).length + " bytes";
    }
}
