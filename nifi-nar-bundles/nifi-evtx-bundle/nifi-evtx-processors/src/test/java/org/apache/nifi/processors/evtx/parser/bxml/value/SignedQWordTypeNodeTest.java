package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class SignedQWordTypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testSignedQWordTypeNode() throws IOException {
        long value = -5L;
        assertEquals(Long.toString(value),
                new SignedQWordTypeNode(testBinaryReaderBuilder.putQWord(value).build(), chunkHeader, parent, -1).getValue());
    }
}
