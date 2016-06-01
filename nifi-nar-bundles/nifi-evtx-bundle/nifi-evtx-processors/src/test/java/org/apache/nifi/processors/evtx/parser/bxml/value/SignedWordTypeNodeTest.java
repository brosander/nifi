package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class SignedWordTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testSignedWordTypeNode() throws IOException {
        short value = -5;
        assertEquals(Short.toString(value),
                new SignedWordTypeNode(testBinaryReaderBuilder.putWord(value).build(), chunkHeader, parent, -1).getValue());
    }
}
