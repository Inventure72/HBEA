/** Stateless, most-significant-bit-first bit operations. */
public final class BitUtils {
    private BitUtils() { }

    public static int[] xor(int[] left, int[] right) {
        requireBits(left, -1);
        requireBits(right, left.length);
        int[] result = new int[left.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = left[i] ^ right[i];
        }
        return result;
    }

    /** Applies a table of 1-based positions without changing the input. */
    public static int[] permute(int[] bits, int[] table) {
        requireBits(bits, -1);
        if (table == null) { throw new IllegalArgumentException("Null permutation table"); }
        int[] result = new int[table.length];
        for (int i = 0; i < table.length; i++) {
            if (table[i] < 1 || table[i] > bits.length) {
                throw new IllegalArgumentException("Permutation position outside input");
            }
            result[i] = bits[table[i] - 1];
        }
        return result;
    }

    public static int[] bytesToBits(byte[] bytes) {
        if (bytes == null) { throw new IllegalArgumentException("Null bytes"); }
        int[] bits = new int[bytes.length * 8];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            for (int j = 0; j < 8; j++) {
                bits[8 * i + j] = (value >> (7 - j)) & 1;
            }
        }
        return bits;
    }

    public static byte[] bitsToBytes(int[] bits) {
        requireBits(bits, -1);
        if (bits.length % 8 != 0) { throw new IllegalArgumentException("Bits must form whole bytes"); }
        byte[] bytes = new byte[bits.length / 8];
        for (int i = 0; i < bits.length; i++) {
            bytes[i / 8] |= (byte) (bits[i] << (7 - i % 8));
        }
        return bytes;
    }

    /** Encodes whole bytes as uppercase hexadecimal. */
    public static String toHex(int[] bits) {
        byte[] bytes = bitsToBytes(bits);
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format(java.util.Locale.ROOT, "%02X", value & 0xFF));
        }
        return hex.toString();
    }

    public static int[] fromHex(String hex) {
        if (hex == null || hex.length() % 2 != 0 || !hex.matches("[0-9A-Fa-f]*")) {
            throw new IllegalArgumentException("Expected an even number of hexadecimal digits");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bytesToBits(bytes);
    }

    static void requireBits(int[] bits, int length) {
        if (bits == null || (length >= 0 && bits.length != length)) {
            throw new IllegalArgumentException("Invalid bit array length; expected " + length);
        }
        for (int bit : bits) {
            if (bit != 0 && bit != 1) { throw new IllegalArgumentException("Bits must be 0 or 1"); }
        }
    }
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
