public class L2Cache {
    private CacheLine[] lines;
    private Processor processor;
    private Memory mem;
    private int nextEvict = 0;

    public L2Cache(Processor p, Memory m) {
        processor = p;
        mem = m;
        lines = new CacheLine[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = new CacheLine();
            lines[i].tag = new Word32();
            lines[i].valid = false;
            lines[i].data = new Word32[8];
            for (int j = 0; j < 8; j++) {
                lines[i].data[j] = new Word32();
            }
        }
    }

    protected Word32 read(Word32 address) {
        int addr = TestConverter.toInt(address);
        int base = addr - (addr % 8);
        int offset = addr % 8;

        for (int i = 0; i < 4; i++) {
            if (lines[i].valid && TestConverter.toInt(lines[i].tag) == base) {  //HIT
                processor.addCycles(20);
                return lines[i].data[offset];
            }
        }

        processor.addCycles(360);
        for (int j = 0; j < 8; j++) {
            Word32 addrs = new Word32();
            TestConverter.fromInt(base + j, addrs);  // - build address for word j
            addrs.copy(mem.address);
            mem.read();
            mem.value.copy(lines[nextEvict].data[j]);            // - fill eviction target
        }
        TestConverter.fromInt(base, lines[nextEvict].tag);
        lines[nextEvict].valid = true;
        int filled = nextEvict;
        nextEvict = (nextEvict + 1) % 4;    // - advance round-robin counter
        return lines[filled].data[offset];
    }

    protected void fillLine(int base, CacheLine target) {
        // search L2 lines for hit
        for (int i = 0; i < 4; i++) {
            if (lines[i].valid && TestConverter.toInt(lines[i].tag) == base) {  // HIT
                processor.addCycles(20);
                for (int j = 0; j < 8; j++) {
                    lines[i].data[j].copy(target.data[j]);  // - copy L2 line into L1's line
                }
                return;
            }
        }

        // MISS - fill from memory into eviction target
        processor.addCycles(370);
        for (int j = 0; j < 8; j++) {
            Word32 addr = new Word32();
            TestConverter.fromInt(base + j, addr);    // - build address for word j
            addr.copy(mem.address);                    // - set memory's address
            mem.read();
            mem.value.copy(lines[nextEvict].data[j]); // - store in L2 eviction target
        }
        TestConverter.fromInt(base, lines[nextEvict].tag);
        lines[nextEvict].valid = true;
        int filled = nextEvict;
        nextEvict = (nextEvict + 1) % 4;              // - advance round-robin counter
        for (int j = 0; j < 8; j++) {
            lines[filled].data[j].copy(target.data[j]); // - copy filled L2 line into L1
        }
    }

    protected void write(Word32 address, Word32 value) {
        int addr = TestConverter.toInt(address);
        int base = addr - (addr % 8);
        int offset = addr % 8;

        // search L2 lines for hit
        for (int i = 0; i < 4; i++) {
            if (lines[i].valid && TestConverter.toInt(lines[i].tag) == base) {  // HIT
                processor.addCycles(20);
                value.copy(lines[i].data[offset]);  // - update L2's cached copy
                address.copy(mem.address);
                value.copy(mem.value);
                mem.write();                         // - write through to memory
                return;
            }
        }

        // MISS - write through to memory only
        processor.addCycles(360);
        address.copy(mem.address);
        value.copy(mem.value);
        mem.write();                                 // - write through to memory
    }

}
