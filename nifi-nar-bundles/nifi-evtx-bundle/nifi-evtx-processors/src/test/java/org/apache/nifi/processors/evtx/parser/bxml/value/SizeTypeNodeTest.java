package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class SizeTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testSizeTypeNodeDWord() throws IOException {
        UnsignedInteger value = UnsignedInteger.fromIntBits(Integer.MAX_VALUE + 132);
        assertEquals(value.toString(),
                new SizeTypeNode(testBinaryReaderBuilder.putDWord(value).build(), chunkHeader, parent, 4).getValue());
    }

    @Test
    public void testSizeTypeNodeQWord() throws IOException {
        UnsignedLong value = UnsignedLong.fromLongBits(Long.MAX_VALUE + 132);
        assertEquals(value.toString(),
                new SizeTypeNode(testBinaryReaderBuilder.putQWord(value).build(), chunkHeader, parent, -1).getValue());
    }
}
