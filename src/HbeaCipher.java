import java.util.Arrays;
import java.util.Objects;

/** Eight-round Feistel cipher with private round keys and injected interfaces. */
public final class HbeaCipher extends AbstractBlockCipher {
    private final int[][] roundKeys;
    private final RoundFunction roundFunction;

    public HbeaCipher(String seed) {
        this(seed, new Sha256KeyGenerator(), new DesRoundFunction());
    }

    public HbeaCipher(String seed, KeyGenerator generator, RoundFunction roundFunction) {
        Objects.requireNonNull(generator, "Key generator");
        this.roundFunction = Objects.requireNonNull(roundFunction, "Round function");
        int[][] segments = generator.generateKeySegments(seed);
        if (segments == null || segments.length != KeyGenerator.SEGMENTS) {
            throw new IllegalArgumentException("Expected eight key segments");
        }
        roundKeys = new int[KeyGenerator.SEGMENTS][];
        // Each 32-bit key segment becomes a 48-bit round key.
        for (int i = 0; i < segments.length; i++) {
            BitUtils.requireBits(segments[i], KeyGenerator.SEGMENT_BITS);
            roundKeys[i] = HbeaTables.expand(segments[i]);
        }
    }

    @Override public String getName() { return "HBEA"; }
    @Override protected int[] encryptBlock(int[] block64) { return process(block64, false); }
    @Override protected int[] decryptBlock(int[] block64) { return process(block64, true); }

    private int[] process(int[] block64, boolean reverse) {
        // Start with IP, then split the block into its two 32-bit halves.
        int[] permuted = HbeaTables.initialPermutation(block64);
        int[] left = Arrays.copyOfRange(permuted, 0, 32);
        int[] right = Arrays.copyOfRange(permuted, 32, 64);
        for (int round = 0; round < roundKeys.length; round++) {
            // Decryption uses the same steps but reads the keys backwards.
            int index = reverse ? roundKeys.length - 1 - round : round;
            int[] function = roundFunction.apply(right.clone(), roundKeys[index].clone());
            BitUtils.requireBits(function, 32);
            int[] nextRight = BitUtils.xor(left, function);
            left = right;
            right = nextRight;
        }
        int[] swapped = new int[64];
        // The last round is followed by a swap and the inverse of IP.
        System.arraycopy(right, 0, swapped, 0, 32);
        System.arraycopy(left, 0, swapped, 32, 32);
        return HbeaTables.finalPermutation(swapped);
    }
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
