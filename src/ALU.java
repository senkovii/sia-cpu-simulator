public class ALU {
    public Word16 instruction = new Word16();
    public Word32 op1 = new Word32();
    public Word32 op2 = new Word32();
    public Word32 result = new Word32();
    public Bit less = new Bit(false);
    public Bit equal = new Bit(false);

    public void doInstruction(){
        int opcode = getOpcode();

        if (opcode == 1)           // add
            { Adder.add(op1,op2,result); }
        else if (opcode == 2)      // and
            { Word32.and(op1, op2, result); }
        else if (opcode == 3)      // multiply
            { Multiplier.multiply(op1,op2,result);}
        else if (opcode == 4)      // left shift
            { Shifter.LeftShift(op1,TestConverter.toInt(op2),result); }
        else if (opcode == 5)      // subtract
            { Adder.subtract(op1, op2, result); }
        else if (opcode == 6)      // or
            { Word32.or(op1, op2, result); }
        else if (opcode == 7)      // right shift
            { Shifter.RightShift(op1,TestConverter.toInt(op2),result); }
        else if (opcode == 11) {  // compare
            Word32 temp = new Word32();
            Adder.subtract(op1, op2, temp);
            temp.getBitN(0, less);
            equal.assign(Word32.equals(op1, op2));
        }
    }

    private int getOpcode() {
        int val = 1, retVal = 0;
        Bit cur = new Bit(false);
        for (int i = 4; i >= 0; i--) {
            instruction.getBitN(i, cur);
            if (cur.getValue())
                retVal += val;
            val *= 2;
        }
        return retVal;
    }
}
