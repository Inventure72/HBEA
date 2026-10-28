import java.util.Scanner;

/** Interactive console demonstration of two interchangeable Cipher objects. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("HBEA - Matteo Giorgetti - CP2 Interfaces Project");
            System.out.print("Official e-mail: ");
            if (!scanner.hasNextLine()) { return; }
            String email = scanner.nextLine();
            System.out.print("Message (1-32 printable ASCII characters): ");
            if (!scanner.hasNextLine()) { return; }
            String message = scanner.nextLine();
            Cipher[] ciphers = {new HbeaCipher(email), new XorCipher(email)};
            for (Cipher cipher : ciphers) { demonstrate(cipher, message); }

            Cipher original = new HbeaCipher(email);
            Cipher differentKey = new HbeaCipher(email + ".different");
            String wrong = differentKey.decrypt(original.encrypt(message));
            System.out.println("\nWrong-key HBEA (different e-mail):");
            System.out.println("Decrypted (escaped): " + escaped(wrong));
            System.out.println("Matches original: " + message.equals(wrong));

            String shortCipher = original.encrypt("Hi Anas");
            System.out.println("\nIndependent blocks: Hi Anas");
            for (int i = 0; i < 4; i++) {
                System.out.println("Block " + (i + 1) + ": " + shortCipher.substring(i * 16, i * 16 + 16));
            }
        } catch (InvalidMessageException | IllegalArgumentException error) {
            System.err.println("Input error: " + error.getMessage());
            System.exit(1);
        }
    }

    private static void demonstrate(Cipher cipher, String message) throws InvalidMessageException {
        SecureMessenger messenger = new SecureMessenger(cipher);
        String encrypted = messenger.send(message);
        System.out.println("\nCipher: " + cipher.getName());
        System.out.println("Ciphertext: " + encrypted);
        System.out.println("Decrypted: " + messenger.receive(encrypted));
        System.out.println("Round trip: " + cipher.verifyRoundTrip(message));
    }

    private static String escaped(String text) {
        StringBuilder result = new StringBuilder();
        for (char character : text.toCharArray()) {
            if (character >= 32 && character <= 126) { result.append(character); }
            else { result.append(String.format(java.util.Locale.ROOT, "\\u%04X", (int) character)); }
        }
        return result.toString();
    }
}

/*
Execution results - Java 21
Command: java -cp build Main
HBEA - Matteo Giorgetti - CP2 Interfaces Project
Official e-mail: mgiorgetti.ieu2024@student.ie.edu
Message (1-32 printable ASCII characters): Meet me at the library at 5 PM!!

Cipher: HBEA
Ciphertext: B16AED86DA4D076D891D90216B221D7CD3EBC1077D89A5466E0397D60A6CFA9F
Decrypted: Meet me at the library at 5 PM!!
Round trip: true

Cipher: XOR
Ciphertext: DB1216D289327A14F70353D2C13A3F58FF1501C7DB263F55E2574686F9123E15
Decrypted: Meet me at the library at 5 PM!!
Round trip: true

Wrong-key HBEA (different e-mail):
Decrypted (escaped): \uFFFD\uFFFD\uFFFDn\uFFFD\uFFFDv\uFFFDJ\uFFFD\u0004\uFFFD\uFFFD,\uFFFD*\uFFFD|m\u0003\uFFFD\u0010 \uFFFD\uFFFD\uFFFD8b\uFFFD\uFFFD\uFFFD\uFFFD
Matches original: false

Independent blocks: Hi Anas
Block 1: D547CFA05FFC8EDF
Block 2: 683D31912C8623EC
Block 3: 683D31912C8623EC
Block 4: 683D31912C8623EC
*/
