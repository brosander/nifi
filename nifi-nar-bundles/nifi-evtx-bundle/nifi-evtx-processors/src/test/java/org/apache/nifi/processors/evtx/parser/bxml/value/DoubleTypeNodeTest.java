package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class DoubleTypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testDoubleTypeNode() throws IOException {
        double value = 1.23456;
        assertEquals(Double.toString(value), new DoubleTypeNode(testBinaryReaderBuilder.putQWord(Double.doubleToLongBits(value)).build(), chunkHeader, parent, -1).getValue());
    }
}
