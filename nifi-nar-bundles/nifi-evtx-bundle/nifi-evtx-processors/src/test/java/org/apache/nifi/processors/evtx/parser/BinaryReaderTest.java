package org.apache.nifi.processors.evtx.parser;

import com.google.common.base.Charsets;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.Assert.*;

/**
 * Created by brosander on 6/1/16.
 */
public class BinaryReaderTest {
    @Test
    public void testRead() throws IOException {
        byte b = 0x23;
        BinaryReader binaryReader = new BinaryReader(new byte[]{b});
        assertEquals(b, binaryReader.read());
        assertEquals(1, binaryReader.getPosition());
    }

    @Test
    public void testPeek() throws IOException {
        byte b = 0x23;
        BinaryReader binaryReader = new BinaryReader(new byte[]{b});
        assertEquals(b, binaryReader.peek());
        assertEquals(0, binaryReader.getPosition());
    }

    @Test
    public void testReadBytesJustLength() throws IOException {
        byte[] bytes = "Hello world".getBytes(Charsets.US_ASCII);
        BinaryReader binaryReader = new BinaryReader(bytes);
        assertArrayEquals(Arrays.copyOfRange(bytes, 0, 5), binaryReader.readBytes(5));
        assertEquals(5, binaryReader.getPosition());
    }

    @Test
    public void testPeekBytes() throws IOException {
        byte[] bytes = "Hello world".getBytes(Charsets.US_ASCII);
        BinaryReader binaryReader = new BinaryReader(bytes);
        assertArrayEquals(Arrays.copyOfRange(bytes, 0, 5), binaryReader.peekBytes(5));
        assertEquals(0, binaryReader.getPosition());
    }

    @Test
    public void testReadBytesBufOffsetLength() throws IOException {
        byte[] bytes = "Hello world".getBytes(Charsets.US_ASCII);
        byte[] buf = new byte[5];

        BinaryReader binaryReader = new BinaryReader(bytes);
        binaryReader.readBytes(buf, 0, 5);
        assertArrayEquals(Arrays.copyOfRange(bytes, 0, 5), buf);
        assertEquals(5, binaryReader.getPosition());
    }

    @Test
    public void testReadGuid() throws IOException {
        byte[] bytes = "0123456789abcdef".getBytes(Charsets.US_ASCII);

        BinaryReader binaryReader = new BinaryReader(bytes);
        assertEquals("33323130-3534-3736-3839-616263646566", binaryReader.readGuid());
        assertEquals(16, binaryReader.getPosition());
    }

    @Test(expected = IOException.class)
    public void testReadStringNotNullTerminated() throws IOException {
        String value = "Hello world";

        BinaryReader binaryReader = new BinaryReader(value.getBytes(Charsets.US_ASCII));
        binaryReader.readString(value.length());
        fail("Expected to fail reading because string isn't null terminated");
    }

    @Test
    public void testReadString() throws IOException {
        String value = "Hello world";
        byte[] bytes = new byte[value.length() + 1];
        System.arraycopy(value.getBytes(Charsets.US_ASCII), 0, bytes, 0, value.length());

        BinaryReader binaryReader = new BinaryReader(bytes);
        assertEquals(value, binaryReader.readString(value.length() + 1));
        assertEquals(value.length() + 1, binaryReader.getPosition());
    }

    @Test
    public void testReadWString() throws IOException {
        String value = "Hello world";
        BinaryReader binaryReader = new BinaryReader(value.getBytes(Charsets.UTF_16LE));

        assertEquals(value, binaryReader.readWString(value.length()));
        assertEquals(value.length() * 2, binaryReader.getPosition());
    }

    @Test
    public void testReadQWord() throws IOException {
        UnsignedLong longValue = UnsignedLong.fromLongBits(Long.MAX_VALUE + 500);
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[8]).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putLong(longValue.longValue());

        BinaryReader binaryReader = new BinaryReader(byteBuffer.array());
        assertEquals(longValue, binaryReader.readQWord());
        assertEquals(8, binaryReader.getPosition());
    }

    @Test
    public void testReadDWord() throws IOException {
        UnsignedInteger intValue = UnsignedInteger.fromIntBits(Integer.MAX_VALUE + 500);
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4]).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(intValue.intValue());

        BinaryReader binaryReader = new BinaryReader(byteBuffer.array());
        assertEquals(intValue, binaryReader.readDWord());
        assertEquals(4, binaryReader.getPosition());
    }

    @Test
    public void testReadDWordBE() throws IOException {
        UnsignedInteger intValue = UnsignedInteger.fromIntBits(Integer.MAX_VALUE + 500);
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[4]).order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putInt(intValue.intValue());

        BinaryReader binaryReader = new BinaryReader(byteBuffer.array());
        assertEquals(intValue, binaryReader.readDWordBE());
        assertEquals(4, binaryReader.getPosition());
    }

    @Test
    public void testReadWord() throws IOException {
        int intValue = Short.MAX_VALUE + 500;
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[2]).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putShort((short) intValue);

        BinaryReader binaryReader = new BinaryReader(byteBuffer.array());
        assertEquals(intValue, binaryReader.readWord());
        assertEquals(2, binaryReader.getPosition());
    }

    @Test
    public void testReadWordBE() throws IOException {
        int intValue = Short.MAX_VALUE + 500;
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[2]).order(ByteOrder.BIG_ENDIAN);
        byteBuffer.putShort((short) intValue);

        BinaryReader binaryReader = new BinaryReader(byteBuffer.array());
        assertEquals(intValue, binaryReader.readWordBE());
        assertEquals(2, binaryReader.getPosition());
    }

    @Test
    public void testReadFileTIme() throws IOException {
        UnsignedLong longValue = UnsignedLong.fromLongBits(Long.MAX_VALUE + 500);
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[8]).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putLong(longValue.longValue());

        BinaryReader binaryReader = new BinaryReader(byteBuffer.array());
        assertEquals(910692730085477L, binaryReader.readFileTime().getTime());
        assertEquals(8, binaryReader.getPosition());
    }

    @Test
    public void testReadAndBase64EncodeBinary() throws IOException {
        String stringValue = "Hello World";
        byte[] binary = stringValue.getBytes(Charsets.US_ASCII);
        BinaryReader binaryReader = new BinaryReader(binary);

        assertEquals(stringValue, new String(Base64.getDecoder().decode(binaryReader.readAndBase64EncodeBinary(stringValue.length())), Charsets.US_ASCII));
        assertEquals(stringValue.length(), binaryReader.getPosition());
    }

    @Test
    public void testSkip() throws IOException {
        BinaryReader binaryReader = new BinaryReader(null);
        binaryReader.skip(10);
        assertEquals(10, binaryReader.getPosition());
    }

    @Test
    public void testReaderPositionConstructor() throws IOException {
        String value = "Hello world";
        BinaryReader binaryReader = new BinaryReader(new BinaryReader(value.getBytes(Charsets.UTF_16LE)), 2);

        assertEquals(value.substring(1), binaryReader.readWString(value.length() - 1));
        assertEquals(value.length() * 2, binaryReader.getPosition());
    }

    @Test
    public void testInputStreamSizeConstructor() throws IOException {
        String value = "Hello world";
        BinaryReader binaryReader = new BinaryReader(new ByteArrayInputStream(value.getBytes(Charsets.UTF_16LE)), 10);

        assertEquals(value.substring(0, 5), binaryReader.readWString(5));
        assertEquals(10, binaryReader.getPosition());
    }
}
