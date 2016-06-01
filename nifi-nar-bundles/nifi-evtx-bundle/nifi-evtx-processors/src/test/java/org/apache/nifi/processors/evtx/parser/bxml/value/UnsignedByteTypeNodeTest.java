package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class UnsignedByteTypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testUnsignedByteTypeNode() throws IOException {
        byte value = -5;
        assertEquals(Integer.toString(Byte.toUnsignedInt(value)),
                new UnsignedByteTypeNode(testBinaryReaderBuilder.put(value).build(), chunkHeader, parent, -1).getValue());
    }
}
