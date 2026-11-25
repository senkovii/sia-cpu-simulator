public class TestConverter {
    public static void fromInt(int value, Word32 result) {
       Bit t= new Bit(true);
       Bit f= new Bit(false);

       if (value >=0 ) {
           // Work right-to-left (bit 31 = least significant in this layout)
           // Each iteration: check if value is odd (remainder 1 = this bit is 1)
           // then divide by 2 to "shift" to the next bit's value
           for (int i = 31; i > 0; i--) {
               result.setBitN(i,value % 2 == 0 ? f : t );
               value /=2;
           }
           // Bit 0 is the sign bit -- positive numbers always have 0 here
           result.setBitN(0, f);
       }
       else {
           // Two's complement for negatives:
           // 1) Convert the positive version of the number into bits
          value *= -1;
          fromInt(value, result);
           // 2) Flip all bits (NOT)
          result.not(result);
           // 3) Add 1, gives the two's complement negative representation
           Word32 one = new Word32();
          fromInt(1,one);
          Adder.add(result,one,result);
       }
    }

    public static int toInt(Word32 value) {
        // Check bit 0 (the sign bit) to determine if the number is negative
        var isNeg = new Bit(false);
        value.getBitN(0,isNeg);
        Word32 positive = new Word32();

        if (isNeg.getValue()) {
            // Reverse the two's complement process to get the magnitude:
            // NOT the bits, then add 1
            value.not(positive);
            Word32 one = new Word32();
            fromInt(1,one);
            Adder.add(positive,one,positive);
        }
        else {
            //copy the value as-is for reading
            value.and(value,positive);
        }

        int val = 1, retVal=0;
        Bit cur = new Bit(true);

        // Read bits right-to-left (bit 31 = 2^0, bit 30 = 2^1, etc.)
        // Each set bit contributes its place value (1, 2, 4, 8...) to the total
        for (int i = 31; i > 0; i--) {
            positive.getBitN(i,cur);
            if (cur.getValue())
                retVal+=val;
            val *=2;    // move to the next power of 2
        }

        // Apply the sign detected at the start
        return retVal * (isNeg.getValue()?-1:1);
    }
}