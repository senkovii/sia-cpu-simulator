public class Word32 {

    private Bit[] bitlist;

    public Word32() {
        bitlist = new Bit[32];
        for (int i = 0; i < 32; i++) {
            bitlist[i] = new Bit(false);    //initializes bit values as false by default
        }
    }

    public Word32(Bit[] in) {
        bitlist = new Bit[32];
        for (int i = 0; i < 32; i++) {
            bitlist[i] = new Bit(in[i].getValue());     //copies bit values from in
        }
    }

    public void getTopHalf(Word16 result) { // sets result = bits 0-15 of this word. use bit.assign
        for (int i = 0; i < 16; i++) {
            result.setBitN(i, bitlist[i]);
        }
    }

    public void getBottomHalf(Word16 result) { // sets result = bits 16-31 of this word. use bit.assign
        for (int i = 0; i < 16; i++) {
            result.setBitN(i, bitlist[i + 16]);
        }
    }

    public void copy(Word32 result) { // sets result's bit to be the same as this. use bit.assign
        for (int i = 0; i < 32; i++) {
            result.bitlist[i].assign(this.bitlist[i].getValue());
        }
    }

    public boolean equals(Word32 other) {
        return equals(this, other);
    }

    public static boolean equals(Word32 a, Word32 b) {
        for (int i = 0; i < 32; i++) {
            if (a.bitlist[i].getValue() != b.bitlist[i].getValue())
                return false;
        }
        return true;
    }

    public void getBitN(int n, Bit result) { // use bit.assign
        result.assign(bitlist[n].getValue());
    }

    public void setBitN(int n, Bit source) { //  use bit.assign
        bitlist[n].assign(source.getValue());
    }

    public void and(Word32 other, Word32 result) {
        and(this, other, result);
    }

    public static void and(Word32 a, Word32 b, Word32 result) {
        for (int i = 0; i < 32; i++) {
            a.bitlist[i].and(b.bitlist[i], result.bitlist[i]);
        }
    }

    public void or(Word32 other, Word32 result) {
        or(this, other, result);
    }

    public static void or(Word32 a, Word32 b, Word32 result) {
        for (int i = 0; i < 32; i++) {
            a.bitlist[i].or(b.bitlist[i], result.bitlist[i]);
        }
    }

    public void xor(Word32 other, Word32 result) {
        xor(this, other, result);
    }

    public static void xor(Word32 a, Word32 b, Word32 result) {
        for (int i = 0; i < 32; i++) {
            a.bitlist[i].xor(b.bitlist[i], result.bitlist[i]);
        }
    }

    public void not(Word32 result) {
        not(this, result);
    }

    public static void not(Word32 a, Word32 result) {
        for (int i = 0; i < 32; i++) {
            a.bitlist[i].not(result.bitlist[i]);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Bit bit : bitlist) {
            sb.append(bit.toString());
            sb.append(",");
        }
        return sb.toString();
    }
}
