package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class Hex32TypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testHex32TypeNode() throws IOException {
        UnsignedInteger value = UnsignedInteger.valueOf(1234);
        String hex = value.toString(16);
        assertEquals(hex, new Hex32TypeNode(testBinaryReaderBuilder.putDWord(value).build(), chunkHeader, parent, -1).getValue().substring(2));
    }
}
