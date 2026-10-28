import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Locale;

/** Hashes a trimmed, lowercase UTF-8 seed into eight consecutive segments. */
public final class Sha256KeyGenerator implements KeyGenerator {
    @Override
    public int[][] generateKeySegments(String seed) {
        if (seed == null || seed.trim().isEmpty()) {
            throw new IllegalArgumentException("Seed must not be null or blank");
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    seed.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            int[] bits = BitUtils.bytesToBits(hash);
            int[][] segments = new int[SEGMENTS][SEGMENT_BITS];
            for (int i = 0; i < SEGMENTS; i++) {
                segments[i] = Arrays.copyOfRange(bits, i * SEGMENT_BITS, (i + 1) * SEGMENT_BITS);
            }
            return segments;
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
