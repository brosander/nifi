package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class NullTypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testNullTypeNode() throws IOException {
        assertEquals("", new NullTypeNode(testBinaryReaderBuilder.build(), chunkHeader, parent, -1).getValue());
    }
}
