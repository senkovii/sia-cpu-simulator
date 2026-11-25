import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that the ALU dispatches each opcode to the right unit
 * and sets the less/equal flags used by compare and the branches.
 */
public class ALUOperationsTest {

    // - loads a fresh ALU with an opcode (bits 0-4, MSB first) and two operands
    private static ALU setup(int opcode, int op1, int op2) {
        ALU alu = new ALU();
        for (int i = 0; i < 5; i++)
            alu.instruction.setBitN(i, new Bit(((opcode >> (4 - i)) & 1) == 1));
        TestConverter.fromInt(op1, alu.op1);
        TestConverter.fromInt(op2, alu.op2);
        return alu;
    }

    private static int run(int opcode, int op1, int op2) {
        ALU alu = setup(opcode, op1, op2);
        alu.doInstruction();
        return TestConverter.toInt(alu.result);
    }

    @Test
    public void testAdd() {
        assertEquals(30, run(1, 18, 12));
        assertEquals(-3, run(1, 4, -7));
    }

    @Test
    public void testAnd() {
        assertEquals(0b1000, run(2, 0b1100, 0b1010));
    }

    @Test
    public void testMultiply() {
        assertEquals(144, run(3, 12, 12));
    }

    @Test
    public void testLeftShift() {
        assertEquals(40, run(4, 5, 3));
    }

    @Test
    public void testSubtract() {
        assertEquals(-9, run(5, 11, 20));
    }

    @Test
    public void testOr() {
        assertEquals(0b1110, run(6, 0b1100, 0b1010));
    }

    @Test
    public void testRightShift() {
        assertEquals(5, run(7, 80, 4));
    }

    @Test
    public void testCompareFlags() {
        // - compare sets less when op1 < op2 and equal when op1 == op2
        ALU lessCase = setup(11, 3, 9);
        lessCase.doInstruction();
        assertTrue(lessCase.less.getValue());
        assertFalse(lessCase.equal.getValue());

        ALU equalCase = setup(11, -4, -4);
        equalCase.doInstruction();
        assertFalse(equalCase.less.getValue());
        assertTrue(equalCase.equal.getValue());

        ALU greaterCase = setup(11, 15, -2);
        greaterCase.doInstruction();
        assertFalse(greaterCase.less.getValue());
        assertFalse(greaterCase.equal.getValue());
    }
}
