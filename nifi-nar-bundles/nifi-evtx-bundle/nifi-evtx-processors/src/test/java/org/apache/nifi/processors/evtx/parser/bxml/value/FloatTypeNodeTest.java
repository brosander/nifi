package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class FloatTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testFloatTypeNode() throws IOException {
        float value = 5.432f;
        assertEquals(Float.toString(value),
                new FloatTypeNode(testBinaryReaderBuilder.putDWord(Float.floatToIntBits(value)).build(), chunkHeader, parent, -1).getValue());
    }
}
