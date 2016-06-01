package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class SignedByteTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testSignedByteTypeNode() throws IOException {
        byte value = -25;
        assertEquals(Byte.toString(value), new SignedByteTypeNode(testBinaryReaderBuilder.put(value).build(), chunkHeader, parent, -1).getValue());
    }
}
