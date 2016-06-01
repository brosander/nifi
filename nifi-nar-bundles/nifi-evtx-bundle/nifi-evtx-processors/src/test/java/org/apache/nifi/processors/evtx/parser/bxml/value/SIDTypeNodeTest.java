package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class SIDTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testSIDTypeNode() throws IOException {
        testBinaryReaderBuilder.put((byte) 4);
        testBinaryReaderBuilder.put((byte) 1);
        testBinaryReaderBuilder.putDWordBE(5);
        testBinaryReaderBuilder.putWordBE(6);
        testBinaryReaderBuilder.putDWord(7);
        assertEquals("S-4-327686-7", new SIDTypeNode(testBinaryReaderBuilder.build(), chunkHeader, parent, -1).getValue());
    }
}
