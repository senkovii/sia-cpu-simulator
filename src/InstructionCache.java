public class InstructionCache {
    private CacheLine line;
    private Processor processor;
    private L2Cache l2;

    public InstructionCache(Processor p, L2Cache l2) {
        processor = p;
        this.l2 = l2;
        line = new CacheLine();
        line.tag = new Word32();
        line.data = new Word32[8];
        for (int i = 0; i < 8; i++) {
            line.data[i] = new Word32();
        }
    }

    protected Word32 read(Word32 address) {
        int addr = TestConverter.toInt(address);
        int base = addr - (addr % 8);
        int offset = addr % 8;

        if (line.valid && TestConverter.toInt(line.tag) == base) {  //HIT
            processor.addCycles(10);
            return line.data[offset];
        } else {    // MISS - L2 handles cycle cost
            l2.fillLine(base, line);                 // - fill entire L1 line from L2
            TestConverter.fromInt(base, line.tag);
            line.valid = true;
            return line.data[offset];
        }
    }
}