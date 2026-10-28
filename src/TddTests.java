import java.util.Arrays;

/** Tests written first for input validation, padding and XOR. */
public final class TddTests {
    private static int passed = 0;
    private static int failed = 0;

    private TddTests() { }

    /** A cipher that leaves blocks unchanged lets us inspect padding directly. */
    private static class IdentityCipher extends AbstractBlockCipher {
        @Override
        protected int[] encryptBlock(int[] block) { return block.clone(); }
        @Override
        protected int[] decryptBlock(int[] block) { return block.clone(); }
        @Override
        public String getName() { return "Identity test fixture"; }
    }

    static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    static void equal(String name, Object expected, Object actual) {
        check(name, expected.equals(actual));
        if (!expected.equals(actual)) {
            System.out.println("  Expected: " + expected + "; actual: " + actual);
        }
    }

    private static void invalidMessage(Cipher cipher, String name, String input) {
        try {
            cipher.encrypt(input);
            check(name, false);
        } catch (InvalidMessageException expected) {
            check(name, true);
        }
    }

    private static void invalidXor(String name, int[] left, int[] right) {
        try {
            BitUtils.xor(left, right);
            check(name, false);
        } catch (IllegalArgumentException expected) {
            check(name, true);
        }
    }

    static void runValidationAndXor() throws InvalidMessageException {
        Cipher identity = new IdentityCipher();
        invalidMessage(identity, "TDD null plaintext", null);
        invalidMessage(identity, "TDD empty plaintext", "");
        invalidMessage(identity, "TDD more than 32 characters", "A".repeat(33));
        invalidMessage(identity, "TDD non-ASCII plaintext", "café");
        invalidMessage(identity, "TDD control character", "a\nb");
        invalidMessage(identity, "TDD embedded NUL", "a\0b");
        equal("TDD short message has NUL padding", "4869" + "00".repeat(30), identity.encrypt("Hi"));
        equal("TDD exactly 32 characters has no padding", "41".repeat(32), identity.encrypt("A".repeat(32)));
        equal("TDD trailing spaces preserved", "Hi  ", identity.decrypt(identity.encrypt("Hi  ")));
        equal("TDD printable boundaries", " ~", identity.decrypt(identity.encrypt(" ~")));
        int[] left = {0, 1, 1, 0};
        int[] right = {1, 1, 0, 0};
        int[] expected = {1, 0, 1, 0};
        check("TDD XOR known bits and unchanged input", Arrays.equals(expected, BitUtils.xor(left, right))
                && Arrays.equals(left, new int[]{0, 1, 1, 0}));
        invalidXor("TDD XOR mismatched lengths", new int[1], new int[2]);
        invalidXor("TDD XOR null left", null, new int[1]);
        invalidXor("TDD XOR null right", new int[1], null);
        invalidXor("TDD XOR invalid bit", new int[]{2}, new int[]{0});
    }

    static void summary() {
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed != 0) { throw new AssertionError("Tests failed: " + failed); }
    }

    public static void main(String[] args) throws InvalidMessageException {
        runValidationAndXor();
        summary();
    }
}

/*
Execution results - Java 21
Command: java -cp build TddTests
PASS TDD null plaintext
PASS TDD empty plaintext
PASS TDD more than 32 characters
PASS TDD non-ASCII plaintext
PASS TDD control character
PASS TDD embedded NUL
PASS TDD short message has NUL padding
PASS TDD exactly 32 characters has no padding
PASS TDD trailing spaces preserved
PASS TDD printable boundaries
PASS TDD XOR known bits and unchanged input
PASS TDD XOR mismatched lengths
PASS TDD XOR null left
PASS TDD XOR null right
PASS TDD XOR invalid bit
RESULT: 15 passed, 0 failed
*/
