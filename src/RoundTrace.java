import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Test/demo code tracing the first block without exposing cipher key fields. */
public final class RoundTrace {
    private RoundTrace() { }

    private static class TracingRoundFunction implements RoundFunction {
        private final RoundFunction function = new DesRoundFunction();
        private int[] previousLeft;
        private int count = 0;

        TracingRoundFunction(int[] firstLeft) { previousLeft = firstLeft.clone(); }

        @Override
        public int[] apply(int[] right32, int[] roundKey48) {
            int[] result = function.apply(right32, roundKey48);
            if (count < 8) {
                int[] nextRight = BitUtils.xor(previousLeft, result);
                count++;
                System.out.println("Round " + count + "  L=" + BitUtils.toHex(right32)
                        + "  R=" + BitUtils.toHex(nextRight));
                previousLeft = right32.clone();
            }
            return result;
        }
    }

    public static void main(String[] args) throws InvalidMessageException {
        String seed = args.length > 0 ? args[0] : "student@student.ie.edu";
        String message = "M"; // First letter of Matteo; remaining bytes are NUL padding.
        byte[] firstBlock = Arrays.copyOf(message.getBytes(StandardCharsets.US_ASCII), 8);
        int[] initial = HbeaTables.initialPermutation(BitUtils.bytesToBits(firstBlock));
        System.out.println("Round trace - first letter of Matteo: M");
        System.out.println("First block: " + BitUtils.toHex(BitUtils.bytesToBits(firstBlock)));
        System.out.println("Round 0  L=" + BitUtils.toHex(Arrays.copyOfRange(initial, 0, 32))
                + "  R=" + BitUtils.toHex(Arrays.copyOfRange(initial, 32, 64)));
        RoundFunction tracing = new TracingRoundFunction(Arrays.copyOfRange(initial, 0, 32));
        Cipher cipher = new HbeaCipher(seed, new Sha256KeyGenerator(), tracing);
        String ciphertext = cipher.encrypt(message);
        System.out.println("First cipher block: " + ciphertext.substring(0, 16));
    }
}

/*
Execution results - Java 21
Command: java -cp build RoundTrace mgiorgetti.ieu2024@student.ie.edu
Round trace - first letter of Matteo: M
First block: 4D00000000000000
Round 0  L=01000101  R=00000100
Round 1  L=00000100  R=86AB5B70
Round 2  L=86AB5B70  R=3617F7F7
Round 3  L=3617F7F7  R=5BA45848
Round 4  L=5BA45848  R=70B8A251
Round 5  L=70B8A251  R=4152808D
Round 6  L=4152808D  R=262032C9
Round 7  L=262032C9  R=B66D5FB5
Round 8  L=B66D5FB5  R=A93596C1
First cipher block: 7B8CBE689EF229C7
*/
