import java.util.Arrays;

/** Completes the supplied simplified DES second-round worksheet. */
public final class DesWorksheet {
    private static final int[] EXPANSION = {4, 1, 2, 3, 2, 3, 4, 1};
    private static final int[] P4 = {2, 4, 3, 1};
    private static final int[] INVERSE_IP = {4, 1, 3, 5, 7, 2, 8, 6};
    private static final int[][] S0 = {{3, 2, 1, 0}, {2, 3, 0, 1}, {0, 1, 3, 2}, {1, 0, 2, 3}};
    private static final int[][] S1 = {{0, 1, 2, 3}, {1, 2, 3, 0}, {2, 3, 0, 1}, {3, 0, 1, 2}};

    private DesWorksheet() { }

    private static int[] lookup(int[] group, int[][] box) {
        int row = group[0] * 2 + group[3];
        int column = group[1] * 2 + group[2];
        int value = box[row][column];
        return new int[]{(value >> 1) & 1, value & 1};
    }

    private static String binary(int[] bits) {
        StringBuilder text = new StringBuilder();
        for (int bit : bits) { text.append(bit); }
        return text.toString();
    }

    public static void main(String[] args) {
        int[] firstRound = {0, 0, 1, 0, 1, 0, 1, 1};
        int[] key = {1, 0, 1, 1, 1, 0, 0, 1};
        int[] left = Arrays.copyOfRange(firstRound, 4, 8); // Swap between rounds.
        int[] right = Arrays.copyOfRange(firstRound, 0, 4);
        int[] expanded = BitUtils.permute(right, EXPANSION);
        int[] mixed = BitUtils.xor(expanded, key);
        int[] s0 = lookup(Arrays.copyOfRange(mixed, 0, 4), S0);
        int[] s1 = lookup(Arrays.copyOfRange(mixed, 4, 8), S1);
        int[] substituted = {s0[0], s0[1], s1[0], s1[1]};
        int[] permuted = BitUtils.permute(substituted, P4);
        int[] nextLeft = BitUtils.xor(left, permuted);
        int[] beforeFinal = new int[8];
        System.arraycopy(nextLeft, 0, beforeFinal, 0, 4);
        System.arraycopy(right, 0, beforeFinal, 4, 4);
        String answer = binary(BitUtils.permute(beforeFinal, INVERSE_IP));
        System.out.println("Simplified DES worksheet");
        System.out.println("After first round: " + binary(firstRound));
        System.out.println("After swap:       " + binary(left) + binary(right));
        System.out.println("K2:               " + binary(key));
        System.out.println("E/P(right):       " + binary(expanded));
        System.out.println("XOR K2:           " + binary(mixed));
        System.out.println("S0 | S1:          " + binary(substituted));
        System.out.println("P4:               " + binary(permuted));
        System.out.println("Left XOR P4:      " + binary(nextLeft));
        System.out.println("Before IP^-1:     " + binary(beforeFinal));
        System.out.println("Ciphered text:    " + answer);
        if (!answer.equals("10101100")) { throw new AssertionError("Worksheet result changed"); }
    }
}

/*
Execution results - Java 21
Command: java -cp build DesWorksheet
Simplified DES worksheet
After first round: 00101011
After swap:       10110010
K2:               10111001
E/P(right):       00010100
XOR K2:           10101101
S0 | S1:          0101
P4:               1100
Left XOR P4:      0111
Before IP^-1:     01110010
Ciphered text:    10101100
*/
