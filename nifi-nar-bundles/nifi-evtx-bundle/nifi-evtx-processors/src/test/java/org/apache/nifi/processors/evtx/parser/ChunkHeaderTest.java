package org.apache.nifi.processors.evtx.parser;

import org.junit.Before;

import java.io.IOException;

/**
 * Created by brosander on 6/2/16.
 */
public class ChunkHeaderTest {
    private int headerOffset = 101;
    private int chunkNumber = 102;
    private ChunkHeader chunkHeader;

    @Before
    public void setup() throws IOException {
        TestBinaryReaderBuilder testBinaryReaderBuilder = new TestBinaryReaderBuilder();
        testBinaryReaderBuilder.putString(ChunkHeader.ELF_CHNK);
        testBinaryReaderBuilder.putQWord(103);
        chunkHeader = new ChunkHeader(testBinaryReaderBuilder.build(), headerOffset, chunkNumber);
    }
}
