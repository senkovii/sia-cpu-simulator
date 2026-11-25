public class Adder {
    public static void subtract(Word32 a, Word32 b, Word32 result) {
        Word32 notB = new Word32();    //Step 1: Create a Word32 to hold NOT(b)

        b.not(notB);    //Step 2: Invert b

        //Step 3: Create a Word32 representing 1
        Word32 one = new Word32();
        one.setBitN(31, new Bit(true));

        //Step 4: Add NOT(b) + 1 to get -b
        Word32 negativeB = new Word32();
        add(notB, one, negativeB);

        //Step 5: Add a + (-b) to get final result
        add(a, negativeB, result);
    }

    public static void add(Word32 a, Word32 b, Word32 result) {

        Bit carry = new Bit(false);

        for (int i = 31; i >= 0; i--) {     //addition carries right to left, so bit 31 -> bit 0
            // Get the bits at position i from both words
            Bit bitA = new Bit(false);
            Bit bitB = new Bit(false);
            a.getBitN(i, bitA);
            b.getBitN(i, bitB);

            // Calculate the sum bit for this position, using temporary Bit objects to store intermediate results

            //Step 1: XOR bitA and bitB → store in sum
            Bit firstXor = new Bit(false);
            bitA.xor(bitB, firstXor);

            //Step 2: XOR sum with carry → final sum bit
            Bit sum = new Bit(false);
            firstXor.xor(carry, sum);

            //Calculate the new carry

            /* implement (a AND b) OR (a AND carry) OR (b AND carry),
            create temporary Bits for each AND operation,
            then OR them together */

            Bit andAB = new Bit(false);
            bitA.and(bitB, andAB);

            Bit andACarry = new Bit(false);
            bitA.and(carry, andACarry);

            Bit andBCarry = new Bit(false);
            bitB.and(carry, andBCarry);

            Bit tempOr = new Bit(false);
            andAB.or(andACarry, tempOr);

            Bit newCarry = new Bit(false);
            tempOr.or(andBCarry, newCarry);

            result.setBitN(i, sum);

            //Update carry for next iteration (copy value, not reference)
            carry.assign(newCarry.getValue());
        }
    }
}
