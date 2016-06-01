package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class WStringTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testWStringTypeNodeLengthArg() throws IOException {
        String value = "testValue";
        assertEquals(value, new WStringTypeNode(testBinaryReaderBuilder.putWString(value).build(), chunkHeader, parent, value.length() * 2).getValue());
    }

    @Test
    public void testWStringTypeNodeNoLengthArg() throws IOException {
        String value = "testValue";
        assertEquals(value, new WStringTypeNode(testBinaryReaderBuilder.putWord(value.length()).putWString(value).build(), chunkHeader, parent, -1).getValue());
    }
}
