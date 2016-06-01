package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class StringTypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testStringLengthArg() throws IOException {
        String value = "string test";
        assertEquals(value, new StringTypeNode(testBinaryReaderBuilder.putString(value).build(), chunkHeader, parent, value.length() + 1).getValue());
    }
    @Test
    public void testStringNoLengthArg() throws IOException {
        String value = "string test";
        BinaryReader binaryReader = testBinaryReaderBuilder.putWord(value.length() + 1).putString(value).build();
        assertEquals(value, new StringTypeNode(binaryReader, chunkHeader, parent, -1).getValue());
    }
}
