package edu.coet.campusnest;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class Passwords {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int ITERATIONS = 210_000;
    private Passwords() {}

    public static String hash(String password) {
        byte[] salt = new byte[16]; RANDOM.nextBytes(salt);
        byte[] derived = derive(password.toCharArray(), salt, ITERATIONS);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(derived);
    }

    public static boolean verify(String password, String stored) {
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4 || !parts[0].equals("pbkdf2")) return false;
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password.toCharArray(), Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1]));
            if (actual.length != expected.length) return false;
            int diff = 0; for (int i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
            return diff == 0;
        } catch (RuntimeException ex) { return false; }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
            try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
            finally { spec.clearPassword(); java.util.Arrays.fill(password, '\0'); }
        } catch (Exception ex) { throw new IllegalStateException("Password hashing is unavailable", ex); }
    }
}
