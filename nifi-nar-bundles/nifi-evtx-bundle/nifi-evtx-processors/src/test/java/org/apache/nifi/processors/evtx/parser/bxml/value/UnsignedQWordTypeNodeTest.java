package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class UnsignedQWordTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testUnsignedQWordTypeNode() throws IOException {
        long value = -5;
        assertEquals(UnsignedLong.fromLongBits(value).toString(),
                new UnsignedQWordTypeNode(testBinaryReaderBuilder.putQWord(value).build(), chunkHeader, parent, -1).getValue());
    }
}
