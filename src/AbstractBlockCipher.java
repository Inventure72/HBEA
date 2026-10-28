import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Template for validation, fixed-length padding and four-block processing. */
public abstract class AbstractBlockCipher implements Cipher {
    @Override
    public final String encrypt(String plaintext) throws InvalidMessageException {
        validate(plaintext);
        // New bytes are automatically zero, giving the required NUL padding.
        byte[] padded = Arrays.copyOf(plaintext.getBytes(StandardCharsets.US_ASCII), 32);
        return BitUtils.toHex(transform(BitUtils.bytesToBits(padded), true));
    }

    /** Overload accepting one complete, NUL-padded 256-bit message. */
    public final String encrypt(int[] bits256) throws InvalidMessageException {
        BitUtils.requireBits(bits256, 256);
        byte[] bytes = BitUtils.bitsToBytes(bits256);
        int length = unpaddedLength(bytes);
        for (int i = 0; i < length; i++) {
            int value = bytes[i] & 0xFF;
            if (value < 32 || value > 126) {
                throw new InvalidMessageException("Message must contain printable ASCII only");
            }
        }
        return encrypt(new String(bytes, 0, length, StandardCharsets.US_ASCII));
    }

    @Override
    public final String decrypt(String cipherHex) {
        if (cipherHex == null || cipherHex.length() != 64) {
            throw new IllegalArgumentException("Ciphertext must contain exactly 64 hex digits");
        }
        byte[] bytes = BitUtils.bitsToBytes(transform(BitUtils.fromHex(cipherHex), false));
        return new String(bytes, 0, unpaddedLength(bytes), StandardCharsets.US_ASCII);
    }

    private static void validate(String plaintext) throws InvalidMessageException {
        if (plaintext == null || plaintext.isEmpty() || plaintext.length() > 32) {
            throw new InvalidMessageException("Message must contain 1 to 32 printable ASCII characters");
        }
        for (int i = 0; i < plaintext.length(); i++) {
            char character = plaintext.charAt(i);
            if (character < 32 || character > 126) {
                throw new InvalidMessageException("Message must contain printable ASCII only");
            }
        }
    }

    private static int unpaddedLength(byte[] bytes) {
        int length = bytes.length;
        while (length > 0 && bytes[length - 1] == 0) { length--; }
        return length;
    }

    private int[] transform(int[] bits, boolean encrypting) {
        int[] output = new int[256];
        for (int offset = 0; offset < bits.length; offset += 64) {
            // The subclass decides how to encrypt or decrypt each block.
            int[] block = Arrays.copyOfRange(bits, offset, offset + 64);
            int[] transformed = encrypting ? encryptBlock(block) : decryptBlock(block);
            BitUtils.requireBits(transformed, 64);
            System.arraycopy(transformed, 0, output, offset, 64);
        }
        return output;
    }

    protected abstract int[] encryptBlock(int[] block64);
    protected abstract int[] decryptBlock(int[] block64);
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
