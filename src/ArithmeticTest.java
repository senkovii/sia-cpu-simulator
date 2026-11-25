import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the bit-level arithmetic units: Adder, Multiplier, and Shifter,
 * plus the TestConverter helper that the other tests rely on.
 */
public class ArithmeticTest {

    // - shorthand for building a Word32 from an int
    private static Word32 w(int value) {
        Word32 word = new Word32();
        TestConverter.fromInt(value, word);
        return word;
    }

    private static int add(int a, int b) {
        Word32 r = new Word32();
        Adder.add(w(a), w(b), r);
        return TestConverter.toInt(r);
    }

    private static int subtract(int a, int b) {
        Word32 r = new Word32();
        Adder.subtract(w(a), w(b), r);
        return TestConverter.toInt(r);
    }

    private static int multiply(int a, int b) {
        Word32 r = new Word32();
        Multiplier.multiply(w(a), w(b), r);
        return TestConverter.toInt(r);
    }

    @Test
    public void testConverterRoundTrip() {
        int[] values = { 0, 1, 2, 37, 255, 1024, 65535, 123456789, -1, -16, -300, -98765 };
        for (int v : values)
            assertEquals(v, TestConverter.toInt(w(v)), "round trip " + v);
    }

    @Test
    public void testAddPositive() {
        assertEquals(15, add(7, 8));
        assertEquals(1000, add(999, 1));
        assertEquals(65536, add(65535, 1));    // - carry ripples across 16 bits
    }

    @Test
    public void testAddNegative() {
        assertEquals(-15, add(-20, 5));
        assertEquals(0, add(-42, 42));
        assertEquals(-50, add(-25, -25));
    }

    @Test
    public void testAddOverflowWrapsToNegative() {
        // - largest positive + 1 sets the sign bit, as in real 32-bit hardware
        Word32 r = new Word32();
        Adder.add(w(Integer.MAX_VALUE), w(1), r);
        Bit sign = new Bit(false);
        r.getBitN(0, sign);
        assertTrue(sign.getValue());
    }

    @Test
    public void testSubtract() {
        assertEquals(6, subtract(10, 4));
        assertEquals(-7, subtract(5, 12));
        assertEquals(0, subtract(33, 33));
        assertEquals(10, subtract(-5, -15));
    }

    @Test
    public void testMultiply() {
        assertEquals(132, multiply(12, 11));
        assertEquals(0, multiply(9, 0));
        assertEquals(77, multiply(77, 1));
        assertEquals(1048576, multiply(1024, 1024));
    }

    @Test
    public void testLeftShift() {
        Word32 r = new Word32();
        Shifter.LeftShift(w(3), 4, r);
        assertEquals(48, TestConverter.toInt(r));
    }

    @Test
    public void testRightShift() {
        Word32 r = new Word32();
        Shifter.RightShift(w(64), 3, r);
        assertEquals(8, TestConverter.toInt(r));
    }

    @Test
    public void testShiftUsesLowestFiveBits() {
        // - 33 = 100001 in binary, so only the low 5 bits (1) count
        Word32 r = new Word32();
        Shifter.LeftShift(w(5), 33, r);
        assertEquals(10, TestConverter.toInt(r));
    }
}
