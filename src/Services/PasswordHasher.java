package Services;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {
    private static final int ITER = 185000;
    private static final int KEY_LEN = 256;
    private static final String ALG = "PBKDF2WithHmacSHA256";

    public static byte[] newSalt() {
        byte[] s = new byte[16];
        new SecureRandom().nextBytes(s);
        return s;
    }

    public static String hash(char[] password, byte[] salt) {
        try {
            var spec = new PBEKeySpec(password, salt, ITER, KEY_LEN);
            var skf = SecretKeyFactory.getInstance(ALG);
            return Base64.getEncoder().encodeToString(skf.generateSecret(spec).getEncoded());
        } catch (Exception e) {
            throw new IllegalStateException("Hashing error", e);
        }
    }
}
