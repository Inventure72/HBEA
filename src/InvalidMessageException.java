/** Checked exception for plaintext outside the assignment's input contract. */
public class InvalidMessageException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidMessageException(String message) {
        super(message);
    }
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
