import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end tests: assemble a program, load it into memory, run it on the processor,
 * and check registers or cycle counts.
 *
 * Notes for writing SIA programs against this processor:
 *   - immediates are 5-bit signed (-16 to 15)
 *   - branch/call target = (PC / 2 + immediate - 1) * 2, so targets are always even indices
 *   - "compare rA rB" sets less when rB < rA (rB is the destination, compared against the source)
 */
public class ProgramExecutionTest {

    private static Processor run(String[] program) {
        var assembled = Assembler.assemble(program);
        var merged = Assembler.finalOutput(assembled);
        var m = new Memory();
        m.load(merged);
        var p = new Processor(m);
        p.run();
        return p;
    }

    // - reads register n from the first syscall 0 dump ("rN:b,b,b,...")
    private static int reg(Processor p, int n) {
        String bits = p.output.get(n).split(":")[1].replace(",", "");
        Word32 w = new Word32();
        for (int i = 0; i < 32; i++)
            w.setBitN(i, new Bit(bits.charAt(i) == '1'));
        return TestConverter.toInt(w);
    }

    // ── Correctness ──────────────────────────────────────────────────────────

    @Test
    public void testNegativeArithmetic() {
        String[] program = {
                "copy -5 r0",       // 0
                "subtract 10 r0",   // 1  r0 = -15
                "copy 6 r1",        // 2
                "multiply r1 r1",   // 3  r1 = 36
                "syscall 0",        // 4
                "halt"              // 5
        };
        var p = run(program);
        assertEquals(-15, reg(p, 0));
        assertEquals(36, reg(p, 1));
    }

    @Test
    public void testCountdownLoopSum() {
        // - sums 10 + 9 + ... + 1 with a loop that runs 10 times
        String[] program = {
                "copy 10 r0",       // 0  counter
                "copy 0 r1",        // 1  sum
                "add r0 r1",        // 2  loop start (even index)
                "subtract 1 r0",    // 3
                "compare 0 r0",     // 4  equal when counter hits 0
                "bne -1",           // 5  (6/2 - 1 - 1) * 2 = 2
                "syscall 0",        // 6
                "halt"              // 7
        };
        var p = run(program);
        assertEquals(0, reg(p, 0));
        assertEquals(55, reg(p, 1));
    }

    @Test
    public void testCallAndReturn() {
        String[] program = {
                "copy 3 r0",        // 0
                "call 2",           // 1  (2/2 + 2 - 1) * 2 = 4
                "syscall 0",        // 2  return lands here
                "halt",             // 3
                "multiply r0 r0",   // 4  subroutine: r0 = r0 * r0
                "return"            // 5
        };
        var p = run(program);
        assertEquals(9, reg(p, 0));
    }

    @Test
    public void testBranchTakenSkipsInstructions() {
        String[] program = {
                "copy 3 r0",        // 0
                "copy 7 r1",        // 1
                "compare r1 r0",    // 2  r0 (3) < r1 (7) sets less
                "blt 2",            // 3  (4/2 + 2 - 1) * 2 = 6
                "copy 15 r2",       // 4  skipped
                "copy 15 r2",       // 5  skipped
                "copy 1 r3",        // 6
                "syscall 0",        // 7
                "halt"              // 8
        };
        var p = run(program);
        assertEquals(0, reg(p, 2));
        assertEquals(1, reg(p, 3));
    }

    @Test
    public void testBranchNotTakenFallsThrough() {
        String[] program = {
                "copy 3 r0",        // 0
                "copy 7 r1",        // 1
                "compare r1 r0",    // 2  less, so greater is not set
                "bgt 2",            // 3  not taken
                "copy 15 r2",       // 4  runs
                "copy 15 r2",       // 5
                "copy 1 r3",        // 6
                "syscall 0",        // 7
                "halt"              // 8
        };
        var p = run(program);
        assertEquals(15, reg(p, 2));
        assertEquals(1, reg(p, 3));
    }

    @Test
    public void testStoreAndLoadThroughCache() {
        String[] program = {
                "copy 10 r2",       // 0
                "add r2 r2",        // 1  r2 = 20
                "add r2 r2",        // 2  r2 = 40 (address)
                "copy 13 r1",       // 3
                "store r1 r2",      // 4  mem[40] = 13
                "load r2 r3",       // 5  r3 = mem[40]
                "add 1 r2",         // 6  r2 = 41
                "store -6 r2",      // 7  mem[41] = -6 (immediate value)
                "copy 0 r5",        // 8
                "add 10 r5",        // 9
                "add 15 r5",        // 10
                "add 15 r5",        // 11 r5 = 40
                "load 1 r5",        // 12 r5 = mem[40 + 1]
                "syscall 0",        // 13
                "halt"              // 14
        };
        var p = run(program);
        assertEquals(13, reg(p, 3));
        assertEquals(-6, reg(p, 5));
    }

    // ── Cycle accounting ─────────────────────────────────────────────────────

    @Test
    public void testSingleLineProgramCycleCount() {
        // - 5 instructions fit in one 8-word cache line:
        //   1 cold miss (370) + 4 L1 hits (4 * 10) + 4 adds (4 * 2) = 418
        String[] program = { "add 1 r0", "add 1 r0", "add 1 r0", "add 1 r0", "halt" };
        var p = run(program);
        assertEquals(418, p.clockCycle);
    }

    @Test
    public void testSecondCacheLineMisses() {
        // - 17 instructions span two lines (16 instructions per line):
        //   2 misses (2 * 370) + 15 L1 hits (15 * 10) + 16 adds (16 * 2) = 922
        String[] program = new String[17];
        for (int i = 0; i < 16; i++)
            program[i] = "add 1 r0";
        program[16] = "halt";
        var p = run(program);
        assertEquals(922, p.clockCycle);
    }

    @Test
    public void testRepeatedDataLoadHitsL2() {
        // - the first load misses and fills an L2 line; a second load from the same line hits
        String[] oneLoad  = { "copy 8 r1", "load r1 r2", "halt" };
        String[] twoLoads = { "copy 8 r1", "load r1 r2", "load r1 r3", "halt" };
        var p1 = run(oneLoad);
        var p2 = run(twoLoads);
        // - the extra load costs one L1 fetch hit (10) plus one L2 data hit (20)
        assertEquals(30, p2.clockCycle - p1.clockCycle);
    }

    @Test
    public void testLoopFetchesHitL1() {
        // - 10 loop passes stay inside the first cache line, so only the first fetch misses
        String[] program = {
                "copy 10 r0",       // 0
                "copy 0 r1",        // 1
                "add r0 r1",        // 2  loop start
                "subtract 1 r0",    // 3
                "compare 0 r0",     // 4
                "bne -1",           // 5
                "halt"              // 6
        };
        var p = run(program);
        // - fetches: 2 setup + 40 loop + 1 halt = 43, one miss (370) and 42 hits (420)
        // - ALU: 10 passes * 3 ops (add, subtract, compare) * 2 = 60
        assertEquals(370 + 420 + 60, p.clockCycle);
    }
}
