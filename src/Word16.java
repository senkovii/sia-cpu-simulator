public class Word16 {

    private Bit[] bitlist;

    public Word16() {
        bitlist = new Bit[16];
        for (int i = 0; i < 16; i++) {
            bitlist[i] = new Bit(false);    //initializes bit values as false by default
        }
    }

    public Word16(Bit[] in) {
        bitlist = new Bit[16];
        for (int i = 0; i < 16; i++) {
            bitlist[i] = new Bit(in[i].getValue());     //copies bit values from in
        }
    }

    public void copy(Word16 result) { // sets the values in "result" to be the same as the values in this instance; use "bit.assign"
        for (int i = 0; i < 16; i++) {
            result.bitlist[i].assign(this.bitlist[i].getValue());
        }
    }

    public void setBitN(int n, Bit source) { // sets the nth bit of this word to "source"
        bitlist[n].assign(source.getValue());
    }

    public void getBitN(int n, Bit result) { // sets result to be the same value as the nth bit of this word
        result.assign(bitlist[n].getValue());
    }

    public boolean equals(Word16 other) { // is other equal to this
        return equals(this, other);
    }

    public static boolean equals(Word16 a, Word16 b) {
        for (int i = 0; i < 16; i++) {
            if (a.bitlist[i].getValue() != b.bitlist[i].getValue())
                return false;
        }
        return true;
    }

    public void and(Word16 other, Word16 result) {
        and(this, other, result);
    }

    public static void and(Word16 a, Word16 b, Word16 result) {
        for (int i = 0; i < 16; i++) {
            a.bitlist[i].and(b.bitlist[i], result.bitlist[i]);
        }
    }

    public void or(Word16 other, Word16 result) {
        or(this, other, result);
    }

    public static void or(Word16 a, Word16 b, Word16 result) {
        for (int i = 0; i < 16; i++) {
            a.bitlist[i].or(b.bitlist[i], result.bitlist[i]);
        }
    }

    public void xor(Word16 other, Word16 result) {
        xor(this, other, result);
    }

    public static void xor(Word16 a, Word16 b, Word16 result) {
        for (int i = 0; i < 16; i++) {
            a.bitlist[i].xor(b.bitlist[i], result.bitlist[i]);
        }
    }

    public void not(Word16 result) {
        not(this, result);
    }

    public static void not(Word16 a, Word16 result) {
        for (int i = 0; i < 16; i++) {
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