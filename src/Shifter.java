public class Shifter {
    public static void LeftShift(Word32 source, int amount, Word32 result) {
        //Step 1: Only consider lowest 5 bits of amount
        amount = amount & 31;

        //Step 2: Loop through each bit position of source
        for (int i = 0; i < 32; i++) {
            int newPosition = i - amount;

            if (newPosition >= 0 && newPosition < 32) {
                Bit currentBit = new Bit(false);
                source.getBitN(i, currentBit);
                result.setBitN(newPosition, currentBit);
            }
            //If newPosition < 0, this bit gets shifted out (ignored)
        }
    }

    public static void RightShift(Word32 source, int amount, Word32 result) {
        amount = amount & 31;

        for (int i = 0; i < 32; i++) {
            int newPosition = i + amount;

            if (newPosition < 32) {
                Bit currentBit = new Bit(false);
                source.getBitN(i, currentBit);
                result.setBitN(newPosition, currentBit);
            }
            //If newPosition >= 32, this bit gets shifted out (ignored)
        }
    }
}
