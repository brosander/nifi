package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by brosander on 6/1/16.
 */
public class BooleanTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testTrue() throws IOException {
        assertTrue(Boolean.parseBoolean(new BooleanTypeNode(testBinaryReaderBuilder.putDWord(1).build(), chunkHeader, parent, -1).getValue()));
    }

    @Test
    public void testFalse() throws IOException {
        assertFalse(Boolean.parseBoolean(new BooleanTypeNode(testBinaryReaderBuilder.putDWord(-1).build(), chunkHeader, parent, -1).getValue()));
    }
}
