package lab;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/** What the caller hands to S03 and C01. It is not a case: its lines belong to none. */
final class Inputs {
    private Inputs() {}

    static SecretKey aesKeyOf256Bits() throws Exception {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);
        return generator.generateKey();
    }

    static KeyPair mlKem768KeyPair() throws Exception {
        return KeyPairGenerator.getInstance("ML-KEM-768").generateKeyPair();
    }
}
