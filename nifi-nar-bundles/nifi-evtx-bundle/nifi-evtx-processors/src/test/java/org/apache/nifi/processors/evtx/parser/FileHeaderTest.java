package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.zip.CRC32;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 6/1/16.
 */
public class FileHeaderTest {
    private final Random random = new Random();
    private FileHeader fileHeader;
    private int oldestChunk = 111;
    private int currentChunkNumber = 112;
    private int nextRecordNumber = 113;
    private int headerSize = 114;
    private int minorVersion = 1;
    private int majorVersion = 3;
    private int headerChunkSize = 4096;
    private int chunkCount = 0;
    private int flags = 340942;

    @Before
    public void setup() throws IOException {
        TestBinaryReaderBuilder testBinaryReaderBuilder = new TestBinaryReaderBuilder();
        testBinaryReaderBuilder.putString(FileHeader.ELF_FILE);
        testBinaryReaderBuilder.putQWord(oldestChunk);
        testBinaryReaderBuilder.putQWord(currentChunkNumber);
        testBinaryReaderBuilder.putQWord(nextRecordNumber);
        testBinaryReaderBuilder.putDWord(headerSize);
        testBinaryReaderBuilder.putWord(minorVersion);
        testBinaryReaderBuilder.putWord(majorVersion);
        testBinaryReaderBuilder.putWord(headerChunkSize);
        testBinaryReaderBuilder.putWord(chunkCount);
        byte[] unused = new byte[75];
        random.nextBytes(unused);
        testBinaryReaderBuilder.put(unused);
        testBinaryReaderBuilder.put((byte) 0);

        CRC32 crc32 = new CRC32();
        crc32.update(testBinaryReaderBuilder.toByteArray());

        testBinaryReaderBuilder.putDWord(flags);
        testBinaryReaderBuilder.putDWord(UnsignedInteger.valueOf(crc32.getValue()));

        fileHeader = new FileHeader(new ByteArrayInputStream(testBinaryReaderBuilder.toByteArray(4096)));
    }

    @Test
    public void testInit() {
        assertEquals(FileHeader.ELF_FILE, fileHeader.getMagicString());
        assertEquals(oldestChunk, fileHeader.getOldestChunk().intValue());
        assertEquals(currentChunkNumber, fileHeader.getCurrentChunkNumber().intValue());
        assertEquals(nextRecordNumber, fileHeader.getNextRecordNumber().intValue());
        assertEquals(headerSize, fileHeader.getHeaderSize().intValue());
        assertEquals(minorVersion, fileHeader.getMinorVersion());
        assertEquals(majorVersion, fileHeader.getMajorVersion());
        assertEquals(headerChunkSize, fileHeader.getHeaderChunkSize());
        assertEquals(chunkCount, fileHeader.getChunkCount());
        assertEquals(flags, fileHeader.getFlags().intValue());
    }
}
