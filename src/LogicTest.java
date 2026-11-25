import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Bit, Word16, and Word32 building blocks.
 * Every other component depends on these, so they check truth tables,
 * bit access, half extraction, and that copy() never aliases.
 */
public class LogicTest {

    // - builds a Word32 from a 32-character string of 0s and 1s
    private static Word32 word32(String bits) {
        Word32 w = new Word32();
        for (int i = 0; i < 32; i++)
            w.setBitN(i, new Bit(bits.charAt(i) == '1'));
        return w;
    }

    // - flattens a word's toString() ("1,0,1,...") into "101..."
    private static String bits(Object word) {
        return word.toString().replace(",", "");
    }

    @Test
    public void testBitTruthTables() {
        boolean[] vals = { false, true };
        for (boolean a : vals) {
            for (boolean b : vals) {
                Bit r = new Bit(false);
                Bit.and(new Bit(a), new Bit(b), r);
                assertEquals(a && b, r.getValue(), "and " + a + " " + b);
                Bit.or(new Bit(a), new Bit(b), r);
                assertEquals(a || b, r.getValue(), "or " + a + " " + b);
                Bit.xor(new Bit(a), new Bit(b), r);
                assertEquals(a != b, r.getValue(), "xor " + a + " " + b);
            }
            Bit r = new Bit(a);
            new Bit(a).not(r);
            assertEquals(!a, r.getValue(), "not " + a);
        }
    }

    @Test
    public void testBitNotIntoItself() {
        // - a bit used as both input and output must still flip
        Bit b = new Bit(true);
        b.not(b);
        assertFalse(b.getValue());
        b.not(b);
        assertTrue(b.getValue());
    }

    @Test
    public void testBitToString() {
        assertEquals("1", new Bit(true).toString());
        assertEquals("0", new Bit(false).toString());
    }

    @Test
    public void testWord32LogicOperations() {
        Word32 a = word32("11110000111100001111000011110000");
        Word32 b = word32("10101010101010101010101010101010");
        Word32 r = new Word32();

        a.and(b, r);
        assertEquals("10100000101000001010000010100000", bits(r));
        a.or(b, r);
        assertEquals("11111010111110101111101011111010", bits(r));
        a.xor(b, r);
        assertEquals("01011010010110100101101001011010", bits(r));
        a.not(r);
        assertEquals("00001111000011110000111100001111", bits(r));
    }

    @Test
    public void testWord32SetAndGetBit() {
        Word32 w = new Word32();
        Bit out = new Bit(false);
        w.setBitN(0, new Bit(true));
        w.setBitN(31, new Bit(true));
        w.getBitN(0, out);
        assertTrue(out.getValue());
        w.getBitN(15, out);
        assertFalse(out.getValue());
        w.getBitN(31, out);
        assertTrue(out.getValue());
    }

    @Test
    public void testWord32Halves() {
        // - processor fetch depends on these: top half = first instruction, bottom = second
        Word32 w = word32("1100110011001100" + "0011001100110011");
        Word16 top = new Word16();
        Word16 bottom = new Word16();
        w.getTopHalf(top);
        w.getBottomHalf(bottom);
        assertEquals("1100110011001100", bits(top));
        assertEquals("0011001100110011", bits(bottom));
    }

    @Test
    public void testWord32CopyDoesNotAlias() {
        // - changing the source after copy() must leave the destination untouched
        Word32 src = word32("10000000000000000000000000000001");
        Word32 dest = new Word32();
        src.copy(dest);
        assertTrue(src.equals(dest));
        src.setBitN(0, new Bit(false));
        assertEquals("10000000000000000000000000000001", bits(dest));
        assertFalse(src.equals(dest));
    }

    @Test
    public void testWord32Equals() {
        Word32 a = word32("01010101010101010101010101010101");
        Word32 b = word32("01010101010101010101010101010101");
        Word32 c = word32("01010101010101010101010101010100");
        assertTrue(Word32.equals(a, b));
        assertFalse(a.equals(c));
    }

    @Test
    public void testWord16OperationsAndCopy() {
        Word16 a = new Word16();
        Word16 b = new Word16();
        for (int i = 0; i < 16; i++) {
            a.setBitN(i, new Bit(i < 8));       // - 1111111100000000
            b.setBitN(i, new Bit(i % 2 == 0));  // - 1010101010101010
        }
        Word16 r = new Word16();
        a.and(b, r);
        assertEquals("1010101000000000", bits(r));
        a.or(b, r);
        assertEquals("1111111110101010", bits(r));
        a.xor(b, r);
        assertEquals("0101010110101010", bits(r));
        a.not(r);
        assertEquals("0000000011111111", bits(r));

        Word16 copy = new Word16();
        a.copy(copy);
        assertTrue(copy.equals(a));
        a.setBitN(15, new Bit(true));
        assertFalse(copy.equals(a));
    }
}
