/** Computes the 32-bit Feistel function from a right half and round key. */
public interface RoundFunction {
    int[] apply(int[] right32, int[] roundKey48);
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
