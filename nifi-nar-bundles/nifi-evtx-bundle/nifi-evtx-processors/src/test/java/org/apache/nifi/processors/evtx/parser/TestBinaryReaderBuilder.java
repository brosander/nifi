package org.apache.nifi.processors.evtx.parser;

import com.google.common.base.Charsets;
import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * Created by brosander on 6/1/16.
 */
public class TestBinaryReaderBuilder {
    private final List<byte[]> data = new ArrayList<>();

    public TestBinaryReaderBuilder put(byte val) {
        data.add(new byte[]{val});
        return this;
    }

    public TestBinaryReaderBuilder put(byte[] bytes) {
        data.add(bytes);
        return this;
    }

    public TestBinaryReaderBuilder putGuid(String guid) {
        byte[] bytes = new byte[16];
        String[] split = guid.split("-");
        int count = 0;
        int offset = 0;
        int[][] indexArrays = BinaryReader.INDEX_ARRAYS;
        for (int i = 0; i < indexArrays.length; i++) {
            String segment = split[i];
            for (int o = 0; o < indexArrays[i].length; o++) {
                int beginIndex = (indexArrays[i][o] * 2) - offset;
                bytes[count++] = (byte) Integer.parseInt(segment.substring(beginIndex, beginIndex + 2), 16);
            }
            offset += segment.length();
        }
        put(bytes);
        return this;
    }

    public TestBinaryReaderBuilder putString(String val) {
        data.add(val.getBytes(Charsets.US_ASCII));
        data.add(new byte[]{0});
        return this;
    }

    public TestBinaryReaderBuilder putWString(String val) {
        data.add(val.getBytes(Charsets.UTF_16LE));
        return this;
    }

    public TestBinaryReaderBuilder putQWord(long longBits) {
        data.add(ByteBuffer.wrap(new byte[8]).order(ByteOrder.LITTLE_ENDIAN).putLong(longBits).array());
        return this;
    }

    public TestBinaryReaderBuilder putQWord(UnsignedLong val) {
        return putQWord(val.longValue());
    }

    public TestBinaryReaderBuilder putDWord(int intBits) {
        data.add(ByteBuffer.wrap(new byte[4]).order(ByteOrder.LITTLE_ENDIAN).putInt(intBits).array());
        return this;
    }

    public TestBinaryReaderBuilder putDWord(UnsignedInteger val) {
        return putDWord(val.intValue());
    }

    public TestBinaryReaderBuilder putDWordBE(int intBits) {
        data.add(ByteBuffer.wrap(new byte[4]).order(ByteOrder.BIG_ENDIAN).putInt(intBits).array());
        return this;
    }

    public TestBinaryReaderBuilder putDWordBE(UnsignedInteger val) {
        return putDWordBE(val.intValue());
    }

    public TestBinaryReaderBuilder putWord(int val) {
        data.add(ByteBuffer.wrap(new byte[2]).order(ByteOrder.LITTLE_ENDIAN).putShort((short)val).array());
        return this;
    }

    public TestBinaryReaderBuilder putWordBE(int val) {
        data.add(ByteBuffer.wrap(new byte[2]).order(ByteOrder.BIG_ENDIAN).putShort((short)val).array());
        return this;
    }

    public TestBinaryReaderBuilder putFileTime(Date date) {
        UnsignedLong javaMillis = UnsignedLong.valueOf(date.getTime());
        UnsignedLong windowsMillis = javaMillis.plus(UnsignedLong.valueOf(BinaryReader.EPOCH_OFFSET));
        UnsignedLong windowsStamp = windowsMillis.times(UnsignedLong.valueOf(10000));
        return putQWord(windowsStamp);
    }

    public TestBinaryReaderBuilder putSystemtime(Calendar calendar) {
        putWord(calendar.get(Calendar.YEAR));
        putWord(calendar.get(Calendar.MONTH));
        putWord(calendar.get(Calendar.DAY_OF_WEEK));
        putWord(calendar.get(Calendar.DAY_OF_MONTH));
        putWord(calendar.get(Calendar.HOUR_OF_DAY));
        putWord(calendar.get(Calendar.MINUTE));
        putWord(calendar.get(Calendar.SECOND));
        putWord(calendar.get(Calendar.MILLISECOND));
        return this;
    }

    public TestBinaryReaderBuilder putBase64EncodedBinary(String base64EncodedBinary) {
        return put(Base64.getDecoder().decode(base64EncodedBinary));
    }

    public BinaryReader build() throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        for (byte[] bytes : data) {
            byteArrayOutputStream.write(bytes);
        }
        return new BinaryReader(byteArrayOutputStream.toByteArray());
    }
}
