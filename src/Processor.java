import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class Processor {
    private Word32[] registers = new Word32[32];
    private Word16 currentInstruction;
    private Word32 op1;                            // - first operand, populated by decode
    private Word32 op2;                            // - second operand, populated by decode
    private Word32 result;                         // - output of execute, read by store
    private int programCounter;                    // - tracks current instruction index
    private int destReg;                           // - destination register index, set by decode, used by store
    private Stack<Integer> callStack;              // - return addresses for call/return instructions
    private boolean halted;                        // - signals run() to stop the fetch/decode/execute/store loop
    private ALU alu = new ALU();
    private int opcode;
    private boolean isImmediate;
    public int clockCycle = 0;
    private InstructionCache instructionCache;
    private L2Cache l2;

    // status flags - set by compare, read by branch instructions
    private boolean flagEqual;
    private boolean flagLess;
    private boolean flagGreater;

    private Memory mem;
    public List<String> output = new LinkedList<>();

    public Processor(Memory m) {
        mem = m;
        for (int i = 0; i < 32; i++) { registers[i] = new Word32(); }
        currentInstruction = new Word16();
        op1 = new Word32();
        op2 = new Word32();
        result = new Word32();
        callStack = new Stack<>();
        l2 = new L2Cache(this, mem);
        instructionCache = new InstructionCache(this, l2);
    }

    public void run() {
        while (!halted) {
            fetch();
            decode();
            execute();
            store();
        }
    }

    protected void fetch() {
        Word32 addr = new Word32();
        TestConverter.fromInt(programCounter / 2, addr);    // - memory address from instruction index
        Word32 fetched = instructionCache.read(addr);
        if (programCounter % 2 == 0)
            fetched.getTopHalf(currentInstruction);
        else
            fetched.getBottomHalf(currentInstruction);
        programCounter++;
        System.out.println("pc=" + (programCounter - 1) + " inst=" + currentInstruction.toString());
    }

    protected void decode() {
        opcode = 0;
        int val = 16;   // - 2^4, MSB of the 5-bit opcode field
        Bit cur = new Bit(false);

        // extract opcode from bits 0-4
        for (int i = 0; i < 5; i++) {
            currentInstruction.getBitN(i, cur);
            if (cur.getValue())
                opcode += val;
            val /= 2;   // - halve each iteration: 16->8->4->2->1
        }

        // extract source (bits 6-10) as integer
        int srcVal = 0;
        val = 16;
        for (int i = 6; i < 11; i++) {
            currentInstruction.getBitN(i, cur);
            if (cur.getValue())
                srcVal += val;
            val /= 2;
        }

        // extract destination (bits 11-15) as integer
        int destVal = 0;
        val = 16;
        for (int i = 11; i <= 15; i++) {
            currentInstruction.getBitN(i, cur);
            if (cur.getValue())
                destVal += val;
            val /= 2;
        }

        Bit formatBit = new Bit(false);
        currentInstruction.getBitN(5, formatBit);
        isImmediate = formatBit.getValue();  // - false = 2R, true = Immediate

        // call/return format: extract 11-bit signed immediate from bits 5-15
        int callVal = 0;
        val = 1024;
        if (opcode == 0 || opcode == 8 || opcode == 9 || opcode == 10 || (opcode >= 12 && opcode <= 17)) {
            for (int i = 5; i <= 15; i++) {
                currentInstruction.getBitN(i, cur);
                if (cur.getValue())
                    callVal += val;
                val /= 2;
            }
            if (callVal >= 1024) {
                callVal -= 2048;    // - sign extend if MSB set
            }
            TestConverter.fromInt(callVal, op1);
        } else if (!isImmediate) {  // 2R format: copy dest and source registers into op1/op2
            registers[destVal].copy(op1);
            registers[srcVal].copy(op2);
        } else {    // immediate format: dest register into op1, sign-extended 5-bit immediate into op2
            int immediate = srcVal;
            if (immediate >= 16) {
                immediate -= 32;    // - sign extend 5-bit value
            }
            registers[destVal].copy(op1);
            TestConverter.fromInt(immediate, op2);
        }

        destReg = destVal;  // - save destination index for store()
    }

    protected void execute() {
        switch (opcode) {
            case 1: case 2: case 3: case 4: case 5: case 6: case 7:
                // load opcode into alu.instruction bits 4-0
                int temp = opcode;
                for (int i = 4; i >= 0; i--) {
                    Bit b = new Bit((temp & 1) == 1);   // - isolate lowest bit
                    alu.instruction.setBitN(i, b);
                    temp = temp >> 1;
                }
                op1.copy(alu.op1);
                op2.copy(alu.op2);
                alu.doInstruction();
                alu.result.copy(result);
                if (opcode == 3)
                    clockCycle += 10;   // - multiply costs 10 cycles
                else
                    clockCycle += 2;
                break;

            case 11:    // compare: same ALU pattern, then set flags
                temp = opcode;
                for (int i = 4; i >= 0; i--) {
                    Bit b = new Bit((temp & 1) == 1);
                    alu.instruction.setBitN(i, b);
                    temp = temp >> 1;
                }
                op1.copy(alu.op1);
                op2.copy(alu.op2);
                alu.doInstruction();
                flagLess = alu.less.getValue();
                flagEqual = alu.equal.getValue();
                flagGreater = !flagLess && !flagEqual;  // - greater if neither less nor equal
                clockCycle += 2;
                break;

            case 18:    // load: 2R uses source register as address; immediate uses dest+immediate as address
                Word32 loadAddr = new Word32();
                if (isImmediate) {
                    Adder.add(op1, op2, loadAddr);  // - address = dest register + immediate
                } else {
                    op2.copy(loadAddr);             // - address = source register
                }
                Word32 loaded = l2.read(loadAddr);
                loaded.copy(result);
                break;

            case 19:    // store: 2R writes source(op2) into address held in dest(op1)
                Word32 storeAddr = new Word32();
                if (isImmediate) {
                    op1.copy(storeAddr);            // - address = dest register
                    l2.write(storeAddr, op2);       // - value = immediate (op2)
                } else {
                    op1.copy(storeAddr);            // - address = dest register (op1)
                    l2.write(storeAddr, op2);       // - value = source register (op2)
                }
                break;

            case 20:    // copy: op2 (source) into result
                op2.copy(result);
                break;

            default:    // halt, branches, call/return: handled in store()
                break;
        }
    }

    protected void printReg() {
        for (int i = 0; i < 32; i++) {
            var line = "r" + i + ":" + registers[i].toString();
            output.add(line);
            System.out.println(line);
        }
    }

    protected void printMem() {
        for (int i = 0; i < 1000; i++) {
            Word32 addr = new Word32();
            Word32 value = new Word32();
            TestConverter.fromInt(i, addr);
            addr.copy(mem.address);
            mem.read();
            mem.value.copy(value);
            var line = i + ":" + value + "(" + TestConverter.toInt(value) + ")";
            output.add(line);
            System.out.println(line);
        }
    }

    protected void store() {
        switch (opcode) {
            case 0:  // halt
                halted = true;
                System.out.println("Clock cycles: " + clockCycle);
                break;
            case 8:  // syscall
                int syscallNum = TestConverter.toInt(op1);
                if (syscallNum == 0)
                    printReg();
                else if (syscallNum == 1)
                    printMem();
                break;
            case 9:  // call: push return address, jump by memory-address-relative immediate
                callStack.push(programCounter);
                programCounter = (programCounter / 2 + TestConverter.toInt(op1) - 1) * 2;
                System.out.println("  branch -> pc=" + programCounter);
                break;
            case 10: // return: pop and restore PC
                programCounter = callStack.pop();
                break;
            case 12: case 13: case 14: case 15: case 16: case 17:
                int target = (programCounter / 2 + TestConverter.toInt(op1) - 1) * 2;
                switch (opcode) {
                    case 12: // BLE
                        if (flagLess || flagEqual) programCounter = target;
                        break;
                    case 13: // BLT
                        if (flagLess) programCounter = target;
                        break;
                    case 14: // BGE
                        if (flagGreater || flagEqual) programCounter = target;
                        break;
                    case 15: // BGT
                        if (flagGreater) programCounter = target;
                        break;
                    case 16: // BEQ
                        if (flagEqual) programCounter = target;
                        break;
                    case 17: // BNE
                        if (!flagEqual) programCounter = target;
                        break;
                }
                break;
            case 11: case 19:
                // - compare and store: no register write
                break;
            default:
                // - write result to destination register
                result.copy(registers[destReg]);
                break;
        }
    }

    public void addCycles(int n) { clockCycle += n; }
}