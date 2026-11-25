public class CacheLine {
    public boolean valid;        // - true if this line holds real data
    public Word32 tag;           // - base address of the first word in this line
    public Word32[] data;        // - the 8 words of cached data

    public CacheLine() {}
}
