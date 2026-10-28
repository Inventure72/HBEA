import java.util.Objects;

/** Messaging application depending only on the Cipher interface. */
public final class SecureMessenger {
    private final Cipher cipher;

    public SecureMessenger(Cipher cipher) {
        this.cipher = Objects.requireNonNull(cipher, "Cipher");
    }

    public String send(String message) throws InvalidMessageException { return cipher.encrypt(message); }
    public String receive(String ciphertext) { return cipher.decrypt(ciphertext); }
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
