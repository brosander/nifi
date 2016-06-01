package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class WStringArrayTypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testWStringArrayTypeNodeLengthArg() throws IOException {
        String[] array = new String[] {"one", "two"};
        String expected = "";
        for (String s : array) {
            expected += "<string>";
            expected += s;
            expected += "</string>";
        }
        String actual = new WStringArrayTypeNode(testBinaryReaderBuilder.putWString(String.join("\u0000", array)).build(), chunkHeader, parent, 14).getValue();
        assertEquals(expected, actual);
    }

    @Test
    public void testWStringArrayTypeNodeNoLengthArg() throws IOException {
        String[] array = new String[] {"one", "two"};
        String expected = "";
        for (String s : array) {
            expected += "<string>";
            expected += s;
            expected += "</string>";
        }
        String actual = new WStringArrayTypeNode(testBinaryReaderBuilder.putWord(14).putWString(String.join("\u0000", array)).build(), chunkHeader, parent, -1).getValue();
        assertEquals(expected, actual);
    }
}
