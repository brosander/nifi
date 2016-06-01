package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class SignedDWordTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testSignedDWordTypeNode() throws IOException {
        int value = -5;
        assertEquals(Integer.toString(value),
                new SignedDWordTypeNode(testBinaryReaderBuilder.putDWord(value).build(), chunkHeader, parent, -1).getValue());
    }
}
