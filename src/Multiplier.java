public class Multiplier {

    public static void multiply(Word32 a, Word32 b, Word32 result) {
        Word32 accumulator = new Word32(); //holds running total
        Word32 shiftedA = new Word32();
        a.copy(shiftedA);

        //Loop through each bit of 'b'
        for (int i = 31; i >= 0; i--) {
            //Check if bit i of b is 1
            Bit currentBit = new Bit(false);
            b.getBitN(i, currentBit);

            //If it's 1, add the shifted version of 'a' to the accumulator
            if (currentBit.getValue() == true) {
                Adder.add(accumulator, shiftedA, accumulator);
            }

            //Shift the multiplicand left by 1 (multiply by 2)
            if (i > 0) {    //only shift if not on last iteration
                Word32 temp = new Word32();
                Adder.add(shiftedA, shiftedA, temp);
                temp.copy(shiftedA);
            }
        }
        accumulator.copy(result);
    }
}
