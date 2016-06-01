package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class Hex64TypeNodeTest extends VariantTypeNodeTestBase {
    @Test
    public void testHex64TypeNode() throws IOException {
        UnsignedLong value = UnsignedLong.fromLongBits(Long.MAX_VALUE + 1234);
        String hex = value.toString(16);
        assertEquals(hex, new Hex64TypeNode(testBinaryReaderBuilder.putQWord(value).build(), chunkHeader, parent, -1).getValue().substring(2));
    }
}
