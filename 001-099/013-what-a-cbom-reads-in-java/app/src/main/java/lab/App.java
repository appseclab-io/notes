package lab;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.mindrot.jbcrypt.BCrypt;

public final class App {
    private App() {}

    // 1. A hash, named in the call
    static String etag(byte[] body) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(body));
    }

    // 2. A cipher, named in the call: the key, and so its size, comes from the caller
    static byte[] seal(SecretKey key, byte[] text) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        byte[] sealed = cipher.doFinal(text);
        return ByteBuffer.allocate(iv.length + sealed.length).put(iv).put(sealed).array();
    }

    // 3. A key pair: the algorithm is named, its size is set in a second call
    static KeyPair signingKey() throws Exception {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(2048);
        return rsa.generateKeyPair();
    }

    // 4. A key agreement on a named curve
    static byte[] agree(PublicKey peer) throws Exception {
        KeyPairGenerator ec = KeyPairGenerator.getInstance("EC");
        ec.initialize(new ECGenParameterSpec("secp256r1"));
        KeyAgreement ecdh = KeyAgreement.getInstance("ECDH");
        ecdh.init(ec.generateKeyPair().getPrivate());
        ecdh.doPhase(peer, true);
        return ecdh.generateSecret();
    }

    // 5. The same call as 1, with the name read from the environment
    static String fingerprint(byte[] file) throws Exception {
        String name = System.getenv().getOrDefault("DIGEST", "SHA-1");
        return HexFormat.of().formatHex(MessageDigest.getInstance(name).digest(file));
    }

    // 6. A signature made inside a dependency: the app names RS256
    static String session(String userId, KeyPair signing) {
        Algorithm rs256 = Algorithm.RSA256(null, (RSAPrivateKey) signing.getPrivate());
        return JWT.create().withSubject(userId).sign(rs256);
    }

    // 7. A password hash made inside a dependency: the app names nothing
    static String storePassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    // 8. TLS: the two ends choose the key exchange when they connect
    static int rates() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.example.com/rates")).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
