package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class UnsignedDWordTypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testUnsignedDWordTypeNode() throws IOException {
        int value = -5;
        assertEquals(UnsignedInteger.fromIntBits(value).toString(),
                new UnsignedDWordTypeNode(testBinaryReaderBuilder.putDWord(value).build(), chunkHeader, parent, -1).getValue());
    }
}
