package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.base.Charsets;
import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNodeTestBase;
import org.junit.Test;

import java.io.IOException;
import java.util.Base64;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class BinaryTypeNodeTest extends BxmlNodeTestBase {
    @Test
    public void testLength() throws IOException {
        String val = "Test String";
        BinaryReader binaryReader = testBinaryReaderBuilder.putDWord(val.length()).putString(val).build();
        assertEquals(Base64.getEncoder().encodeToString(val.getBytes(Charsets.US_ASCII)), new BinaryTypeNode(binaryReader, chunkHeader, parent, -1).getValue());
    }

    @Test(expected = IOException.class)
    public void testInvalidStringLength() throws IOException {
        String val = "Test String";
        BinaryReader binaryReader = testBinaryReaderBuilder.putDWord(UnsignedInteger.fromIntBits(Integer.MAX_VALUE + 1)).putString(val).build();
        assertEquals(Base64.getEncoder().encodeToString(val.getBytes(Charsets.US_ASCII)), new BinaryTypeNode(binaryReader, chunkHeader, parent, -1).getValue());
    }

    @Test
    public void testNoLength() throws IOException {
        String val = "Test String";
        BinaryReader binaryReader = testBinaryReaderBuilder.putString(val).build();
        assertEquals(Base64.getEncoder().encodeToString(val.getBytes(Charsets.US_ASCII)), new BinaryTypeNode(binaryReader, chunkHeader, parent, val.length()).getValue());
    }
}
