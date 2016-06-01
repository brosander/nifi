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
    public static final long EPOCH_OFFSET = 11644473600000L;
    public static final int[][] INDEX_ARRAYS = new int[][]{{3, 2, 1, 0}, {5, 4}, {7, 6}, {8, 9}, {10, 11, 12, 13, 14, 15}};
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
        byte[] result = peekBytes(length);
        position += length;
        return result;
    }

    public void readBytes(byte[] buf, int offset, int length) throws IOException {
        System.arraycopy(bytes, position, buf, offset, length);
        position += length;
    }

    public String readGuid() throws IOException {
        StringBuilder result = new StringBuilder();
        int maxIndex = 0;
        for (int[] indexArray : INDEX_ARRAYS) {
            for (int index : indexArray) {
                maxIndex = Math.max(maxIndex, index);
                result.append(String.format("%02X", bytes[position + index]).toLowerCase());
            }
            result.append("-");
        }
        result.setLength(result.length() - 1);
        position += (maxIndex + 1);
        return result.toString();
    }

    public String readString(int length) throws IOException {
        StringBuilder result = new StringBuilder(length - 1);
        boolean foundNull = false;
        int exclusiveLastIndex = position + length;
        for (int i = position; i < exclusiveLastIndex; i++) {
            byte b = bytes[i];
            if (b == 0) {
                foundNull = true;
                break;
            }
            result.append((char) b);
        }
        if (!foundNull) {
            throw new IOException("Expected null terminated string");
        }
        position += length;
        return result.toString();
    }

    public String readWString(int length) throws IOException {
        int numBytes = length * 2;
        String result = Charsets.UTF_16LE.decode(ByteBuffer.wrap(bytes, position, numBytes)).toString();
        position += numBytes;
        return result;
    }

    public UnsignedLong readQWord() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, position, 8);
        position += 8;
        return UnsignedLong.fromLongBits(byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getLong());
    }

    public UnsignedInteger readDWord() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, position, 4);
        position += 4;
        return UnsignedInteger.fromIntBits(byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getInt());
    }

    public UnsignedInteger readDWordBE() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes, position, 4);
        position += 4;
        return UnsignedInteger.fromIntBits(byteBuffer.order(ByteOrder.BIG_ENDIAN).getInt());
    }

    public int readWord() throws IOException {
        byte[] bytes = new byte[4];
        readBytes(bytes, 0, 2);
        return ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public int readWordBE() throws IOException {
        byte[] bytes = new byte[4];
        readBytes(bytes, 2, 2);
        return ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getInt();
    }

    public Date readFileTime() throws IOException {
        //see http://integriography.wordpress.com/2010/01/16/using-phython-to-parse-and-present-windows-64-bit-timestamps/
        UnsignedLong hundredsOfNanosecondsSinceJan11601 = readQWord();
        long millisecondsSinceJan11601 = hundredsOfNanosecondsSinceJan11601.dividedBy(UnsignedLong.valueOf(10000)).longValue();
        long millisecondsSinceEpoch = millisecondsSinceJan11601 - EPOCH_OFFSET;
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
