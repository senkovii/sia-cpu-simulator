import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the Assembler picks the right instruction format for each line
 * and packs 16-bit instructions into 32-bit memory lines.
 */
public class AssemblerEncodingTest {

    private static String one(String line) {
        return Assembler.assemble(new String[] { line })[0];
    }

    @Test
    public void testTwoRegisterFormat() {
        // - opcode 5 | format 0 | source r3 | dest r9
        assertEquals("00101" + "0" + "00011" + "01001", one("subtract r3 r9"));
        // - opcode 19 | format 0 | source r31 | dest r0
        assertEquals("10011" + "0" + "11111" + "00000", one("store r31 r0"));
    }

    @Test
    public void testImmediateFormatPositive() {
        // - opcode 6 | format 1 | immediate 9 | dest r12
        assertEquals("00110" + "1" + "01001" + "01100", one("or 9 r12"));
    }

    @Test
    public void testImmediateFormatNegative() {
        // - -2 as a 5-bit two's complement value is 11110
        assertEquals("10100" + "1" + "11110" + "00100", one("copy -2 r4"));
    }

    @Test
    public void testCallReturnFormat() {
        // - opcode 17 | 11-bit immediate -3
        assertEquals("10001" + "11111111101", one("bne -3"));
        assertEquals("01001" + "00000000110", one("call 6"));
        assertEquals("01000" + "00000000001", one("syscall 1"));
    }

    @Test
    public void testNoOperandInstructions() {
        assertEquals("0".repeat(16), one("halt"));
        assertEquals("01010" + "0".repeat(11), one("return"));
    }

    @Test
    public void testFinalOutputPairsInstructions() {
        String[] assembled = Assembler.assemble(new String[] { "add 1 r0", "add 2 r0", "add 3 r0", "add 4 r0" });
        String[] lines = Assembler.finalOutput(assembled);
        assertEquals(2, lines.length);
        assertEquals(assembled[0] + assembled[1], lines[0]);
        assertEquals(assembled[2] + assembled[3], lines[1]);
    }

    @Test
    public void testFinalOutputPadsOddCountWithHalt() {
        String[] assembled = Assembler.assemble(new String[] { "copy 1 r0", "copy 2 r1", "halt" });
        String[] lines = Assembler.finalOutput(assembled);
        assertEquals(2, lines.length);
        assertEquals(assembled[2] + "0".repeat(16), lines[1]);
        for (String line : lines)
            assertEquals(32, line.length());
    }
}
