package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.apache.commons.io.Charsets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.Date;

/**
 * Created by brosander on 5/26/16.
 */
public class BinaryReader {
    private final InputStream inputStream;
    private int implicitOffset;

    public BinaryReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public int read() throws IOException {
        int read = inputStream.read();
        if (read >= 0) {
            implicitOffset++;
        }
        return read;
    }

    public int peek() throws IOException {
        inputStream.mark(1);
        try {
            return inputStream.read();
        } finally {
            inputStream.reset();
        }
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] bytes = new byte[length];
        readBytes(bytes, 0, length);
        return bytes;
    }

    public void readBytes(byte[] buf, int offset, int length) throws IOException {
        int read = 0;
        while (read < length) {
            read += inputStream.read(buf, read + offset, length - read);
        }
        implicitOffset += read;
    }

    public String readGuid() throws IOException {
        byte[] bytes = readBytes(16);
        int[][] indexArrays = { { 3, 2, 1, 0 }, { 5, 4 }, { 7, 6 }, { 8, 9 }, { 10, 11, 12, 13, 14, 15 } };
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
            result.append((char)b);
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

    public long skip(long bytes) throws IOException {
        long result = inputStream.skip(bytes);
        implicitOffset += result;
        return result;
    }

    public long getImplicitOffset() {
        return implicitOffset;
    }

    public InputStream getInputStream() {
        return new TrackingInputStream();
    }

    private class TrackingInputStream extends InputStream {
        int lastMark = -1;

        @Override
        public int read() throws IOException {
            int read = inputStream.read();
            if (read != -1) {
                implicitOffset++;
            }
            return read;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int read = inputStream.read(b);
            if (read > 0) {
                implicitOffset += read;
            }
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = inputStream.read(b, off, len);
            if (read > 0) {
                implicitOffset += read;
            }
            return read;
        }

        @Override
        public boolean markSupported() {
            return inputStream.markSupported();
        }

        @Override
        public synchronized void reset() throws IOException {
            if (lastMark == -1) {
                throw new IOException("Call to reset when last mark not set");
            }
            try {
                inputStream.reset();
                implicitOffset = lastMark;
            } catch (IOException e) {
                lastMark = -1;
                throw e;
            }
        }

        @Override
        public synchronized void mark(int readlimit) {
            lastMark = implicitOffset;
            inputStream.mark(readlimit);
        }
    }
}
