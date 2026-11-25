public class Bit {

    private boolean truthval;

    public Bit(boolean value) {
        truthval = value;
    }

    public boolean getValue() { return truthval; }

    public void assign(boolean value) { truthval = value; }

    public void and(Bit b2, Bit result) {
        and(this, b2, result);
    }

    public static void and(Bit b1, Bit b2, Bit result) {
        if (b2.getValue() == true) {
            if (b1.getValue() == true) //if p and q are both true, then true
                result.assign(true);
            else
                result.assign(false);
        } else //every other case = false
            result.assign(false);
    }

    public void or(Bit b2, Bit result) {
        or(this, b2, result);
    }

    public static void or(Bit b1, Bit b2, Bit result) {
        if (b2.getValue() == false) {
            if (b1.getValue() == false) //if p and q are both false, then false
                result.assign(false);
            else
                result.assign(true);
        } else //every other case = true
            result.assign(true);
    }

    public void xor(Bit b2, Bit result) {
        xor(this, b2, result);
    }

    public static void xor(Bit b1, Bit b2, Bit result) {
        if (b2.getValue() != b1.getValue())   //true when one is true and the other is false
            result.assign(true);
        else
            result.assign(false);
    }

    public static void not(Bit b2, Bit result) {
        if (b2.getValue() == true)  //set to opposite truthval
            result.assign(false);
        else
            result.assign(true);
    }

    public void not(Bit result) {
        not(this, result);
    }

    public String toString() {
        if (getValue() == true)
            return "1";
        else
            return "0";
    }
}
