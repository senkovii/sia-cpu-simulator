import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static Processor runProgram(String[] program) {
        var assembled = Assembler.assemble(program);
        var merged    = Assembler.finalOutput(assembled);
        var m         = new Memory();
        m.load(merged);
        var p = new Processor(m);
        p.run();
        return p;
    }

    // ── Phase 1: Cycle Counting ───────────────────────────────────────────────

    @Test
    public void testHaltAloneHasCycles() {
        // a single halt still costs cycles for the fetch through L1/L2
        String[] program = { "halt" };
        var p = runProgram(program);
        assertTrue(p.clockCycle > 0,
                "clock should advance even for a single halt fetch");
    }

    @Test
    public void testALUCycleCost() {
        // two adds + halt -- each add costs 2 cycles
        String[] program = {
                "add 1 r0",
                "add 1 r0",
                "syscall 0"
        };
        var p = runProgram(program);
        // r0 should be 2
        assertEquals("r0:0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,0,",
                p.output.getFirst());
        // at least 4 cycles from the two ALU ops
        assertTrue(p.clockCycle >= 4, "two adds should cost at least 4 ALU cycles");
    }

    @Test
    public void testMultiplyCycleCost() {
        // multiply costs 10 cycles, add costs 2 -- difference should be visible
        String[] program_mul = { "multiply 2 r0", "syscall 0" };
        String[] program_add = { "add 2 r0",      "syscall 0" };
        var p_mul = runProgram(program_mul);
        var p_add = runProgram(program_add);
        assertTrue(p_mul.clockCycle > p_add.clockCycle,
                "multiply should cost more cycles than add");
    }

    // ── Phase 3: L1 + L2 ─────────────────────────────────────────────────────

    @Test
    public void testColdStartMissesL1AndL2() {
        // first instruction fetch always misses both L1 and L2 (370 cycles)
        // plus the fetch itself (10 cycles on the return from fillLine)
        // total for first fetch should reflect a full miss
        String[] program = { "halt" };
        var p = runProgram(program);
        // full miss = 370, so clock should be at least 370
        assertTrue(p.clockCycle >= 370,
                "cold start should incur a full L1+L2 miss (370 cycles)");
    }

}