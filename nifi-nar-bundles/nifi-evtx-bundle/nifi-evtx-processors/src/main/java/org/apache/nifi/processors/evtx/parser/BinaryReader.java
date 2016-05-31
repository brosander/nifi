package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.apache.commons.io.Charsets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

/**
 * Created by brosander on 5/26/16.
 */
public class BinaryReader {
    private final byte[] bytes;
    private int position;

    public BinaryReader(BinaryReader binaryReader, int position) {
        this.bytes = binaryReader.bytes;
        this.position = position;
    }

    public BinaryReader(InputStream inputStream, int size) throws IOException {
        byte[] bytes = new byte[size];
        int read = 0;
        while (read < size) {
            read += inputStream.read(bytes, read, size - read);
        }
        this.bytes = bytes;
        this.position = 0;
    }

    public BinaryReader(byte[] bytes) {
        this.bytes = bytes;
        this.position = 0;
    }

    public int read() throws IOException {
        return bytes[position++];
    }

    public int peek() throws IOException {
        return bytes[position];
    }

    public byte[] peekBytes(int length) throws IOException {
        return Arrays.copyOfRange(bytes, position, position + length);
    }

    public byte[] readBytes(int length) throws IOException {
        try {
            return peekBytes(length);
        } finally {
            position += length;
        }
    }

    public void readBytes(byte[] buf, int offset, int length) throws IOException {
        try {
            System.arraycopy(bytes, position, buf, offset, length);
        } finally {
            position += length;
        }
    }

    public String readGuid() throws IOException {
        byte[] bytes = readBytes(16);
        int[][] indexArrays = {{3, 2, 1, 0}, {5, 4}, {7, 6}, {8, 9}, {10, 11, 12, 13, 14, 15}};
        StringBuilder result = new StringBuilder();
        for (int[] indexArray : indexArrays) {
            for (int index : indexArray) {
                result.append(String.format("%02X", bytes[index]).toLowerCase());
            }
            result.append("-");
        }
        result.setLength(result.length() - 1);
        return result.toString();
    }

    public String readString(int length) throws IOException {
        StringBuilder result = new StringBuilder(length - 1);
        boolean foundNull = false;
        byte[] stringBytes = readBytes(length);
        for (byte b : stringBytes) {
            if (b == 0) {
                foundNull = true;
                break;
            }
            result.append((char) b);
        }
        if (!foundNull) {
            throw new IOException("Expected null terminated string");
        }
        return result.toString();
    }

    public String readWString(int length) throws IOException {
        return new String(readBytes(length * 2), Charsets.UTF_16LE);
    }

    public UnsignedLong readQWord() throws IOException {
        return UnsignedLong.fromLongBits(ByteBuffer.wrap(readBytes(8)).order(ByteOrder.LITTLE_ENDIAN).getLong());
    }

    public UnsignedInteger readDWord() throws IOException {
        return UnsignedInteger.fromIntBits(ByteBuffer.wrap(readBytes(4)).order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    public UnsignedInteger readDWordBE() throws IOException {
        return UnsignedInteger.fromIntBits(ByteBuffer.wrap(readBytes(4)).order(ByteOrder.BIG_ENDIAN).getInt());
    }

    public UnsignedInteger readWord() throws IOException {
        byte[] bytes = new byte[4];
        readBytes(bytes, 0, 2);
        return UnsignedInteger.fromIntBits(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    public UnsignedInteger readWordBE() throws IOException {
        byte[] bytes = new byte[4];
        readBytes(bytes, 0, 2);
        return UnsignedInteger.fromIntBits(ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt());
    }

    public Date readFileTime() throws IOException {
        //see http://integriography.wordpress.com/2010/01/16/using-phython-to-parse-and-present-windows-64-bit-timestamps/
        UnsignedLong hundredsOfNanosecondsSinceJan11601 = readQWord();
        long millisecondsSinceJan11601 = hundredsOfNanosecondsSinceJan11601.dividedBy(UnsignedLong.valueOf(10000)).longValue();
        long millisecondsSinceEpoch = millisecondsSinceJan11601 - 11644473600000L;
        return new Date(millisecondsSinceEpoch);
    }

    public String readAndBase64EncodeBinary(int length) throws IOException {
        return Base64.getEncoder().encodeToString(readBytes(length));
    }

    public void skip(int bytes) throws IOException {
        position += bytes;
    }

    public int getPosition() {
        return position;
    }
}
