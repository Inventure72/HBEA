import java.util.Objects;

/** Demonstration cipher XOR-ing each block with the first 64 key bits. */
public final class XorCipher extends AbstractBlockCipher {
    private final int[] key64;

    public XorCipher(String seed) { this(seed, new Sha256KeyGenerator()); }

    public XorCipher(String seed, KeyGenerator generator) {
        int[][] segments = Objects.requireNonNull(generator, "Key generator").generateKeySegments(seed);
        if (segments == null || segments.length != KeyGenerator.SEGMENTS) {
            throw new IllegalArgumentException("Expected eight key segments");
        }
        for (int[] segment : segments) { BitUtils.requireBits(segment, KeyGenerator.SEGMENT_BITS); }
        key64 = new int[64];
        System.arraycopy(segments[0], 0, key64, 0, 32);
        System.arraycopy(segments[1], 0, key64, 32, 32);
    }

    @Override public String getName() { return "XOR"; }
    @Override protected int[] encryptBlock(int[] block64) {
        BitUtils.requireBits(block64, 64);
        return BitUtils.xor(block64, key64);
    }
    @Override protected int[] decryptBlock(int[] block64) { return encryptBlock(block64); }
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
