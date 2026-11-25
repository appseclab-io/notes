package lab;

import java.nio.charset.StandardCharsets;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.modes.GCMModeCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

/** BouncyCastle 1.87, light-weight API: a cipher whose key size is in the code as an array length. */
public final class Aes187 {
    private Aes187() {}

    private static final byte[] DATA = "note 013".getBytes(StandardCharsets.UTF_8);

    // [N01] AES-GCM with a 256-bit key
    static String gcm() throws Exception {
        KeyParameter key = new KeyParameter(new byte[32]);
        GCMModeCipher cipher = GCMBlockCipher.newInstance(AESEngine.newInstance());
        cipher.init(true, new AEADParameters(key, 128, new byte[12]));
        byte[] out = new byte[cipher.getOutputSize(DATA.length)];
        cipher.doFinal(out, cipher.processBytes(DATA, 0, DATA.length, out, 0));
        return cipher.getAlgorithmName() + ", key " + key.getKeyLength() * 8 + " bits";
    }
}
