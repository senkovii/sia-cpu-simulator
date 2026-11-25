import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the word-addressed Memory class: reads, writes,
 * program loading, and that stored values are copies rather than references.
 */
public class MemorySystemTest {

    private static void write(Memory m, int address, int value) {
        TestConverter.fromInt(address, m.address);
        TestConverter.fromInt(value, m.value);
        m.write();
    }

    private static int read(Memory m, int address) {
        TestConverter.fromInt(address, m.address);
        m.read();
        return TestConverter.toInt(m.value);
    }

    @Test
    public void testWriteThenRead() {
        Memory m = new Memory();
        write(m, 123, 4567);
        assertEquals(4567, read(m, 123));
    }

    @Test
    public void testAddressesAreIndependent() {
        Memory m = new Memory();
        write(m, 10, 1);
        write(m, 11, -2);
        write(m, 999, 3);    // - highest valid address
        assertEquals(1, read(m, 10));
        assertEquals(-2, read(m, 11));
        assertEquals(3, read(m, 999));
    }

    @Test
    public void testUnwrittenMemoryReadsZero() {
        Memory m = new Memory();
        assertEquals(0, read(m, 500));
    }

    @Test
    public void testStoredValueIsCopied() {
        // - changing mem.value after write() must not change what is stored
        Memory m = new Memory();
        write(m, 50, 77);
        TestConverter.fromInt(-1, m.value);
        assertEquals(77, read(m, 50));
    }

    @Test
    public void testLoadPlacesLinesAtSequentialAddresses() {
        Memory m = new Memory();
        String zero = "0".repeat(32);
        String five = "0".repeat(29) + "101";
        String allOnes = "1".repeat(32);
        m.load(new String[] { zero, five, allOnes });
        assertEquals(0, read(m, 0));
        assertEquals(5, read(m, 1));
        assertEquals(-1, read(m, 2));
    }

    @Test
    public void testLoadRejectsWrongLength() {
        Memory m = new Memory();
        assertThrows(IllegalArgumentException.class,
                () -> m.load(new String[] { "0101" }));
    }
}
