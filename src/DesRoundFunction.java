/** DES expansion, XOR, S-box substitution and P permutation. */
public final class DesRoundFunction implements RoundFunction {
    @Override
    public int[] apply(int[] right32, int[] roundKey48) {
        BitUtils.requireBits(right32, 32);
        BitUtils.requireBits(roundKey48, 48);
        int[] mixed = BitUtils.xor(HbeaTables.expand(right32), roundKey48);
        return HbeaTables.permuteRound(HbeaTables.substitute(mixed));
    }
}

/*
Execution results - Java 21
Command: sh scripts/test.sh (this class is exercised by Tests)
RESULT: 69 passed, 0 failed
*/
