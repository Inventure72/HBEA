import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/** Unit tests and Given-When-Then scenarios from the assignment. */
public final class Tests {
    private static final String SEED = "student@student.ie.edu";
    private static final String MESSAGE = "Meet me at the library at 5 PM!!";
    private static final String CIPHERTEXT =
            "AB980FB8275358AE1D411D5FF38B89E5DCAD224E7282B8A8FF7A945EE97A5D40";

    private Tests() { }

    public static void main(String[] args) throws InvalidMessageException {
        TddTests.runValidationAndXor();
        keyScenarios();
        roundVectors();
        cipherScenarios();
        utilityTests();
        TddTests.summary();
    }

    private static void keyScenarios() {
        KeyGenerator generator = new Sha256KeyGenerator();
        int[][] segments = generator.generateKeySegments(SEED);
        String[] expected = {"6C099BA0", "9261A2A3", "516842CB", "93139F39",
                "17514087", "47F298EF", "D359D5E8", "55D4B9F0"};
        StringBuilder complete = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            String hex = BitUtils.toHex(segments[i]);
            TddTests.equal("BDD Given sample seed, When hashed, Then KS" + (i + 1), expected[i], hex);
            complete.append(hex);
        }
        TddTests.equal("BDD complete SHA-256 vector",
                "6C099BA09261A2A3516842CB93139F391751408747F298EFD359D5E855D4B9F0", complete.toString());
        TddTests.check("BDD spaces and capitals produce the same segments",
                Arrays.deepEquals(segments, generator.generateKeySegments(" Student@Student.IE.edu ")));
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            TddTests.check("BDD email normalization is independent of system language",
                    Arrays.deepEquals(generator.generateKeySegments("I@IE.EDU"),
                            generator.generateKeySegments("i@ie.edu")));
        } finally {
            Locale.setDefault(original);
        }
        boolean rejected = false;
        try { generator.generateKeySegments(null); }
        catch (IllegalArgumentException expectedError) { rejected = true; }
        TddTests.check("Null seed rejected", rejected);
        rejected = false;
        try { generator.generateKeySegments("   "); }
        catch (IllegalArgumentException expectedError) { rejected = true; }
        TddTests.check("Blank seed rejected", rejected);
    }

    private static void roundVectors() {
        int[][] segments = new Sha256KeyGenerator().generateKeySegments(SEED);
        int[] key = HbeaTables.expand(segments[0]);
        TddTests.equal("RK1 expansion", "358053CF7D00", BitUtils.toHex(key));
        int[] block = BitUtils.bytesToBits("Meet me ".getBytes(StandardCharsets.US_ASCII));
        int[] permuted = HbeaTables.initialPermutation(block);
        int[] left = Arrays.copyOfRange(permuted, 0, 32);
        int[] right = Arrays.copyOfRange(permuted, 32, 64);
        TddTests.equal("IP left L0", "6F086F67", BitUtils.toHex(left));
        TddTests.equal("IP right R0", "00FE2100", BitUtils.toHex(right));
        TddTests.equal("E(R0)", "0017FC102800", BitUtils.toHex(HbeaTables.expand(right)));
        int[] mixed = BitUtils.xor(HbeaTables.expand(right), key);
        TddTests.equal("E(R0) XOR RK1", "3597AFDF5500", BitUtils.toHex(mixed));
        int[] substituted = HbeaTables.substitute(mixed);
        TddTests.equal("Eight S-boxes", "D688919D", BitUtils.toHex(substituted));
        TddTests.check("S1 example 001101 gives 1101",
                Arrays.equals(new int[]{1, 1, 0, 1}, Arrays.copyOfRange(substituted, 0, 4)));
        TddTests.equal("P permutation", "6B80A973", BitUtils.toHex(HbeaTables.permuteRound(substituted)));
        RoundFunction function = new DesRoundFunction();
        int[] output = function.apply(right, key);
        TddTests.equal("Complete round function F", "6B80A973", BitUtils.toHex(output));
        TddTests.equal("After round 1 L1", "00FE2100", BitUtils.toHex(right));
        TddTests.equal("After round 1 R1", "0488C614", BitUtils.toHex(BitUtils.xor(left, output)));
        TddTests.check("Final permutation reverses IP", Arrays.equals(block, HbeaTables.finalPermutation(permuted)));
    }

    private static void cipherScenarios() throws InvalidMessageException {
        Cipher hbea = new HbeaCipher(SEED);
        TddTests.equal("BDD sample seed and message give exact ciphertext", CIPHERTEXT, hbea.encrypt(MESSAGE));
        TddTests.equal("BDD sample ciphertext decrypts to original", MESSAGE, hbea.decrypt(CIPHERTEXT));
        TddTests.equal("Lowercase ciphertext accepted", MESSAGE, hbea.decrypt(CIPHERTEXT.toLowerCase(Locale.ROOT)));
        Cipher[] ciphers = {hbea, new XorCipher(SEED)};
        for (Cipher cipher : ciphers) {
            String name = cipher.getName();
            TddTests.check("BDD " + name + " inherited default round-trip", cipher.verifyRoundTrip("Hi Anas"));
            SecureMessenger messenger = new SecureMessenger(cipher);
            TddTests.equal("BDD same messenger works with " + name, MESSAGE, messenger.receive(messenger.send(MESSAGE)));
            Cipher other;
            if (name.equals("HBEA")) { other = new HbeaCipher("other@student.ie.edu"); }
            else { other = new XorCipher("other@student.ie.edu"); }
            TddTests.check("BDD " + name + " different email fails to recover message",
                    !MESSAGE.equals(other.decrypt(cipher.encrypt(MESSAGE))));
            String shortCipher = cipher.encrypt("Hi Anas");
            TddTests.check(name + " repeated zero blocks encrypt identically",
                    shortCipher.substring(16, 32).equals(shortCipher.substring(32, 48))
                    && shortCipher.substring(32, 48).equals(shortCipher.substring(48, 64)));
            Random random = new Random(20261028);
            boolean roundTrips = true;
            for (int sample = 0; sample < 100; sample++) {
                StringBuilder message = new StringBuilder();
                for (int j = 0; j < 1 + sample % 32; j++) { message.append((char) (32 + random.nextInt(95))); }
                roundTrips = roundTrips && cipher.verifyRoundTrip(message.toString());
            }
            TddTests.check(name + " 100 deterministic varied-message round trips", roundTrips);
            invalidCiphertext(cipher, name + " null ciphertext", null);
            invalidCiphertext(cipher, name + " short ciphertext", "ABCD");
            invalidCiphertext(cipher, name + " non-hex ciphertext", "Z".repeat(64));
        }
        AbstractBlockCipher overloaded = new HbeaCipher(SEED);
        byte[] padded = Arrays.copyOf(MESSAGE.getBytes(StandardCharsets.US_ASCII), 32);
        int[] bits = BitUtils.bytesToBits(padded);
        TddTests.equal("Overloaded encrypt agrees with String method", CIPHERTEXT, overloaded.encrypt(bits));
        padded = Arrays.copyOf("Hi".getBytes(StandardCharsets.US_ASCII), 32);
        TddTests.equal("Overloaded encrypt accepts short padded message", hbea.encrypt("Hi"),
                overloaded.encrypt(BitUtils.bytesToBits(padded)));
        int[][] suppliedSegments = new Sha256KeyGenerator().generateKeySegments(SEED);
        KeyGenerator fixedGenerator = new FixedKeyGenerator(suppliedSegments);
        Cipher injected = new HbeaCipher(SEED, fixedGenerator, new DesRoundFunction());
        Cipher xor = new XorCipher(SEED, fixedGenerator);
        String xorBefore = xor.encrypt(MESSAGE);
        for (int[] segment : suppliedSegments) { Arrays.fill(segment, 0); }
        TddTests.equal("HBEA owns its keys after constructor", CIPHERTEXT, injected.encrypt(MESSAGE));
        TddTests.equal("XOR owns its key after constructor", xorBefore, xor.encrypt(MESSAGE));
    }

    /** Test double makes key ownership observable without exposing cipher fields. */
    private static class FixedKeyGenerator implements KeyGenerator {
        private final int[][] segments;
        FixedKeyGenerator(int[][] segments) { this.segments = segments; }
        @Override
        public int[][] generateKeySegments(String seed) { return segments; }
    }

    private static void invalidCiphertext(Cipher cipher, String name, String input) {
        boolean rejected = false;
        try { cipher.decrypt(input); }
        catch (IllegalArgumentException expected) { rejected = true; }
        TddTests.check(name, rejected);
    }

    private static void utilityTests() {
        byte[] bytes = {0, 1, 127, (byte) 128, (byte) 255};
        TddTests.equal("Byte bit order and signed-byte handling", "00017F80FF", BitUtils.toHex(BitUtils.bytesToBits(bytes)));
        TddTests.check("Bytes/bits round trip", Arrays.equals(bytes, BitUtils.bitsToBytes(BitUtils.bytesToBits(bytes))));
        TddTests.equal("Hex decoding", "00017F80FF", BitUtils.toHex(BitUtils.fromHex("00017f80ff")));
        boolean rejected = false;
        try { BitUtils.bitsToBytes(new int[7]); }
        catch (IllegalArgumentException expected) { rejected = true; }
        TddTests.check("Partial byte rejected", rejected);
        rejected = false;
        try { BitUtils.permute(new int[]{0}, new int[]{0}); }
        catch (IllegalArgumentException expected) { rejected = true; }
        TddTests.check("Permutation requires 1-based positions", rejected);
        TddTests.check("Empty XOR is well-defined", BitUtils.xor(new int[0], new int[0]).length == 0);
    }
}

/*
Execution results - Java 21
Command: java -cp build Tests
PASS XOR owns its key after constructor
PASS Byte bit order and signed-byte handling
PASS Bytes/bits round trip
PASS Hex decoding
PASS Partial byte rejected
PASS Permutation requires 1-based positions
PASS Empty XOR is well-defined
RESULT: 69 passed, 0 failed
*/
