public class Memory {
    public Word32 address= new Word32();
    public Word32 value = new Word32();

    private final Word32[] dram= new Word32[1000];

    // converts the address Word32 into a usable int index for dram[]
    public int addressAsInt() {
        int val = 1, retVal = 0;
        Bit cur = new Bit(false);

        for (int i = 31; i >= 0; i--) {
            address.getBitN(i, cur);
            if (cur.getValue())
                retVal += val;
            val *= 2;
        }
        return retVal;
    }

    public Memory() {}

    // to use: set mem.address, call read(), then retrieve from mem.value
    public void read() {
        int i = addressAsInt();
        if (dram[i] == null)
            dram[i] = new Word32();
        dram[i].copy(value);    // reads dram[address] into value
    }

    // to use: set mem.address AND mem.value, then call write()
    public void write() {
        int i = addressAsInt();
        if (dram[i] == null)
            dram[i] = new Word32();
        value.copy(dram[i]);    // writes value into dram[address]
    }

    // loads assembled 32-bit strings into dram[] starting at index 0
    // each string becomes one Word32 — meaning each slot holds TWO instructions
    public void load(String[] data) {
        for (int i = 0; i < data.length; i++) {
            if (data[i].length() != 32)
                throw new IllegalArgumentException("invalid length");

            dram[i] = new Word32();

            for (int j = 0; j < 32; j++) {
                char c = data[i].charAt(j);
                Bit b;
                if (c == '1')
                   b  = new Bit(true);
                else
                   b = new Bit(false);
                dram[i].setBitN(j,b);
            }
        }
    }

}
