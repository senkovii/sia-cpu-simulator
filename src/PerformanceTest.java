import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmarks for SIA cache architecture.
 * Tests three programs and measures clock cycles:
 *   1. Sum 1..100 via loop
 *   2. Sum an array of 100 integers stored in memory (addresses 200-299)
 *   3. Sum a linked list of 100 integers (nodes at addresses 400-599)
 */
public class PerformanceTest {

    private static Processor runProgram(String[] program) {
        var assembled = Assembler.assemble(program);
        var merged    = Assembler.finalOutput(assembled);
        var m         = new Memory();
        m.load(merged);
        var p         = new Processor(m);
        p.run();
        return p;
    }

    // -------------------------------------------------------------------------
    // Test 1: Sum 1..100 in a loop (no memory access in loop body)
    //
    // Register use:
    //   r0 = accumulator (result)
    //   r1 = counter (1..100)
    //   r2 = limit (100)
    //
    // Branch target at instruction 10 (even).
    // bgt at instruction 13: (14/2 + (-1) - 1)*2 = (7-2)*2 = 10. Correct.
    // -------------------------------------------------------------------------
    @Test
    public void testSumLoop() {
        String[] program = {
            // Build 100 in r2: 15*7 - 5 = 100
            "copy 15 r2",       // 0
            "add 15 r2",        // 1
            "add 15 r2",        // 2
            "add 15 r2",        // 3
            "add 15 r2",        // 4
            "add 15 r2",        // 5
            "add 15 r2",        // 6
            "subtract 5 r2",    // 7  r2 = 100
            "copy 0 r0",        // 8  sum = 0
            "copy 1 r1",        // 9  counter = 1
            // --- LOOP (starts at even index 10) ---
            "add r1 r0",        // 10 sum += counter
            "compare r2 r1",    // 11 compare 100(dest) to counter(src) -> flagGreater if 100>counter
            "add 1 r1",         // 12 counter++
            "bgt -1",           // 13 if 100 > counter, branch to 10
            // bgt imm=-1: (14/2 + (-1) - 1)*2 = (7-2)*2 = 10. OK.
            "halt",             // 14
        };

        var p = runProgram(program);

        // Expected sum = 1+2+...+100 = 5050
        // Verify via syscall 0 output would show r0=5050, but we check clockCycle here
        System.out.println("=== Test 1: Sum Loop ===");
        System.out.println("Clock cycles: " + p.clockCycle);
        assertTrue(p.clockCycle > 0, "clock cycle should be positive");
    }

    // -------------------------------------------------------------------------
    // Test 2: Array sum (store 1..100 at addresses 200..299, then sum)
    //
    // Phase 1 - Fill: store values 1..100 at memory addresses 200..299
    // Phase 2 - Sum:  load each value and accumulate
    //
    // Register use:
    //   r0 = sum accumulator
    //   r1 = value counter (1..100)
    //   r2 = current address
    //   r3 = 100 (limit)
    //   r4 = 200 (base address)
    //   r5 = 300 (end address sentinel)
    //   r6 = temp for loaded value
    //
    // Fill loop at 14, bgt at 18: (19/2 + (-1) - 1)*2 = (9-2)*2 = 14. OK.
    // Sum loop at 22, bgt at 26:  (27/2 + (-1) - 1)*2 = (13-2)*2 = 22. OK.
    // -------------------------------------------------------------------------
    @Test
    public void testArraySum() {
        String[] program = {
            // Build 100 in r3
            "copy 15 r3",       // 0
            "add 15 r3",        // 1
            "add 15 r3",        // 2
            "add 15 r3",        // 3
            "add 15 r3",        // 4
            "add 15 r3",        // 5
            "add 15 r3",        // 6
            "subtract 5 r3",    // 7  r3 = 100
            // Build 200 in r4: copy r3 then add r3
            "copy r3 r4",       // 8  r4 = 100
            "add r3 r4",        // 9  r4 = 200
            // Build 300 in r5
            "copy r4 r5",       // 10 r5 = 200
            "add r3 r5",        // 11 r5 = 300
            // Fill loop setup
            "copy 1 r1",        // 12 value = 1
            "copy r4 r2",       // 13 addr = 200
            // --- FILL LOOP (starts at even index 14) ---
            "store r1 r2",      // 14 mem[addr] = value
            "add 1 r1",         // 15 value++
            "add 1 r2",         // 16 addr++
            "compare r3 r1",    // 17 compare 100(dest) to value(src) -> flagGreater if 100>value
            "bgt -1",           // 18 if 100 > value, branch to 14
            // bgt imm=-1: (19/2 + (-1) - 1)*2 = (9-2)*2 = 14. OK.
            // Sum loop setup
            "copy 0 r0",        // 19 sum = 0
            "copy r4 r2",       // 20 addr = 200
            "copy 0 r6",        // 21 temp = 0 (padding for even alignment)
            // --- SUM LOOP (starts at even index 22) ---
            "load r2 r6",       // 22 r6 = mem[addr]
            "add r6 r0",        // 23 sum += r6
            "add 1 r2",         // 24 addr++
            "compare r5 r2",    // 25 compare 300(dest) to addr(src) -> flagGreater if 300>addr
            "bgt -1",           // 26 if 300 > addr, branch to 22
            // bgt imm=-1: (27/2 + (-1) - 1)*2 = (13-2)*2 = 22. OK.
            "halt",             // 27
        };

        var p = runProgram(program);

        System.out.println("=== Test 2: Array Sum ===");
        System.out.println("Clock cycles: " + p.clockCycle);
        assertTrue(p.clockCycle > 0);
    }

    // -------------------------------------------------------------------------
    // Test 3: Linked list sum
    //
    // Node layout at addresses 400..599 (100 nodes, 2 words each):
    //   node i: value  at 400 + 2*i
    //           next   at 400 + 2*i + 1  (= addr of node i+1, or 0 for last)
    //
    // Phase 1 - Build: write value and next-pointer for each node
    // Phase 2 - Traverse: follow next pointers, accumulate values
    //
    // Register use:
    //   r0  = sum accumulator
    //   r1  = node value counter (1..100)
    //   r2  = current node value-address (400, 402, 404, ...)
    //   r6  = temp loaded value
    //   r7  = current traversal address
    //   r8  = null sentinel (0)
    //   r9  = 100
    //   r10 = 400 (base address, = 100 * 4 via leftshift 2)
    //   r11 = temp for next-pointer slot address
    //   r12 = temp for next node address
    //
    // Build loop at 14, bgt at 23: (24/2 + (-4) - 1)*2 = (12-5)*2 = 14. OK.
    // Traverse loop at 30, bne at 35: (36/2 + (-2) - 1)*2 = (18-3)*2 = 30. OK.
    // -------------------------------------------------------------------------
    @Test
    public void testLinkedListSum() {
        String[] program = {
            // Build 100 in r9
            "copy 15 r9",       // 0
            "add 15 r9",        // 1
            "add 15 r9",        // 2
            "add 15 r9",        // 3
            "add 15 r9",        // 4
            "add 15 r9",        // 5
            "add 15 r9",        // 6
            "subtract 5 r9",    // 7  r9 = 100
            // Build 400 in r10: 100 << 2 = 400
            "copy r9 r10",      // 8  r10 = 100
            "leftshift 2 r10",  // 9  r10 = 400
            // Build loop setup
            "copy 1 r1",        // 10 value = 1
            "copy r10 r2",      // 11 r2 = 400 (cur val addr)
            // Pad to even (clear sum for later use)
            "copy 0 r0",        // 12 r0 = 0
            "copy r10 r2",      // 13 r2 = 400 (re-set for clarity)
            // --- BUILD LOOP (starts at even index 14) ---
            "copy r2 r11",      // 14 r11 = cur val addr
            "store r1 r11",     // 15 mem[val_addr] = value
            "add 1 r11",        // 16 r11 = cur next-ptr addr (val_addr + 1)
            "copy r2 r12",      // 17 r12 = cur val addr
            "add 2 r12",        // 18 r12 = next node val addr (cur + 2)
            "store r12 r11",    // 19 mem[next_ptr_addr] = next node addr
            "add 1 r1",         // 20 value++
            "add 2 r2",         // 21 r2 = next node addr
            "compare r9 r1",    // 22 compare 100(dest) to value(src) -> flagGreater if 100>value
            "bgt -4",           // 23 if 100 > value, branch to 14
            // bgt imm=-4: (24/2 + (-4) - 1)*2 = (12-5)*2 = 14. OK.
            // Fix last node's next pointer to 0 (null)
            "subtract 1 r2",    // 24 r2 = last node's next-ptr slot
            "store 0 r2",       // 25 mem[last_next_slot] = 0 (null)
            // Traverse setup
            "copy r10 r7",      // 26 r7 = 400 (start of list)
            "copy 0 r8",        // 27 r8 = 0 (null sentinel)
            // Pad + clear sum
            "copy 0 r0",        // 28 sum = 0
            "copy r10 r7",      // 29 r7 = 400 (re-set traverse pointer)
            // --- TRAVERSE LOOP (starts at even index 30) ---
            "load r7 r6",       // 30 r6 = mem[r7] = node value
            "add r6 r0",        // 31 sum += r6
            "add 1 r7",         // 32 r7 = next-ptr slot
            "load r7 r7",       // 33 r7 = mem[r7] = next node addr
            "compare r8 r7",    // 34 compare 0(dest=r8) to r7(src) -> flagEqual if r7==0
            "bne -2",           // 35 if r7 != 0, branch to 30
            // bne imm=-2: (36/2 + (-2) - 1)*2 = (18-3)*2 = 30. OK.
            "halt",             // 36
        };

        var p = runProgram(program);

        System.out.println("=== Test 3: Linked List Sum ===");
        System.out.println("Clock cycles: " + p.clockCycle);
        assertTrue(p.clockCycle > 0);
    }

    // -------------------------------------------------------------------------
    // Comparison: print all three cycle counts together for the report
    // -------------------------------------------------------------------------
    @Test
    public void testCompareAll() {
        // --- Program 1 ---
        String[] p1 = {
            "copy 15 r2", "add 15 r2", "add 15 r2", "add 15 r2",
            "add 15 r2", "add 15 r2", "add 15 r2", "subtract 5 r2",
            "copy 0 r0", "copy 1 r1",
            "add r1 r0", "compare r2 r1", "add 1 r1", "bgt -1",
            "halt",
        };

        // --- Program 2 ---
        String[] p2 = {
            "copy 15 r3", "add 15 r3", "add 15 r3", "add 15 r3",
            "add 15 r3", "add 15 r3", "add 15 r3", "subtract 5 r3",
            "copy r3 r4", "add r3 r4", "copy r4 r5", "add r3 r5",
            "copy 1 r1", "copy r4 r2",
            "store r1 r2", "add 1 r1", "add 1 r2",
            "compare r3 r1", "bgt -1",
            "copy 0 r0", "copy r4 r2", "copy 0 r6",
            "load r2 r6", "add r6 r0", "add 1 r2",
            "compare r5 r2", "bgt -1",
            "halt",
        };

        // --- Program 3 ---
        String[] p3 = {
            "copy 15 r9", "add 15 r9", "add 15 r9", "add 15 r9",
            "add 15 r9", "add 15 r9", "add 15 r9", "subtract 5 r9",
            "copy r9 r10", "leftshift 2 r10",
            "copy 1 r1", "copy r10 r2",
            "copy 0 r0", "copy r10 r2",
            "copy r2 r11", "store r1 r11", "add 1 r11",
            "copy r2 r12", "add 2 r12", "store r12 r11",
            "add 1 r1", "add 2 r2",
            "compare r9 r1", "bgt -4",
            "subtract 1 r2", "store 0 r2",
            "copy r10 r7", "copy 0 r8",
            "copy 0 r0", "copy r10 r7",
            "load r7 r6", "add r6 r0", "add 1 r7",
            "load r7 r7", "compare r8 r7", "bne -2",
            "halt",
        };

        var proc1 = runProgram(p1);
        var proc2 = runProgram(p2);
        var proc3 = runProgram(p3);

        System.out.println("\n===== PERFORMANCE COMPARISON =====");
        System.out.printf("Program 1 (Sum Loop):    %,d clock cycles%n", proc1.clockCycle);
        System.out.printf("Program 2 (Array Sum):   %,d clock cycles%n", proc2.clockCycle);
        System.out.printf("Program 3 (Linked List): %,d clock cycles%n", proc3.clockCycle);
        System.out.printf("Array/Loop ratio:        %.2fx%n",
                (double) proc2.clockCycle / proc1.clockCycle);
        System.out.printf("LinkedList/Loop ratio:   %.2fx%n",
                (double) proc3.clockCycle / proc1.clockCycle);
        System.out.println("==================================\n");

        assertTrue(proc1.clockCycle > 0);
        assertTrue(proc2.clockCycle > 0);
        assertTrue(proc3.clockCycle > 0);
        // Memory-intensive programs should cost more than the pure-register loop
        assertTrue(proc2.clockCycle > proc1.clockCycle,
                "Array sum (with loads) should cost more than pure loop");
        assertTrue(proc3.clockCycle > proc1.clockCycle,
                "Linked list (with loads) should cost more than pure loop");
    }
}
