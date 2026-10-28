/** Contract shared by interchangeable messaging ciphers. */
public interface Cipher {
    String encrypt(String plaintext) throws InvalidMessageException;
    String decrypt(String cipherHex);
    String getName();

    default boolean verifyRoundTrip(String plaintext) throws InvalidMessageException {
        return plaintext.equals(decrypt(encrypt(plaintext)));
    }
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
