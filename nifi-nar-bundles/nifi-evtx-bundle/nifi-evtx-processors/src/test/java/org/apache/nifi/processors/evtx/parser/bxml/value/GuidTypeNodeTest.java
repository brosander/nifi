package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class GuidTypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testGuidTypeNode() throws IOException {
        String guid = "a1b2c3d4-e5f6-a7b8-c9da-ebf001121314";
        assertEquals(guid, new GuidTypeNode(testBinaryReaderBuilder.putGuid(guid).build(), chunkHeader, parent, -1).getValue());
    }
}
