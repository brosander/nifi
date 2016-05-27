package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * Created by brosander on 5/24/16.
 */
public class EventLogTest {
    @Test
    public void testParseHeader() throws IOException {
        try (FileInputStream inputStream = new FileInputStream("/Users/brosander/Downloads/winlogs/system-logs.evtx")) {
            EventLog eventLog = new EventLog(inputStream);
            FileHeader fileHeader = eventLog.getFileHeader();
            assertEquals("ElfFile", fileHeader.getMagicString());
            assertEquals(UnsignedLong.fromLongBits(0L), fileHeader.getOldestChunk());
            assertEquals(UnsignedLong.fromLongBits(33L), fileHeader.getCurrentChunkNumber());
            assertEquals(UnsignedLong.fromLongBits(4315L), fileHeader.getNextRecordNumber());
            assertEquals(UnsignedInteger.fromIntBits(128), fileHeader.getHeaderSize());
            assertEquals(UnsignedInteger.fromIntBits(1), fileHeader.getMinorVersion());
            assertEquals(UnsignedInteger.fromIntBits(3), fileHeader.getMajorVersion());
            assertEquals(UnsignedInteger.fromIntBits(4096), fileHeader.getHeaderChunkSize());
            assertEquals(UnsignedInteger.fromIntBits(34), fileHeader.getChunkCount());
            assertEquals("", fileHeader.getUnused1());
            assertEquals(UnsignedInteger.fromIntBits(0), fileHeader.getFlags());
            assertEquals(UnsignedInteger.valueOf(3575959108L), fileHeader.getChecksum());
            eventLog.getFileHeader().forEachRemaining(chunkHeader -> {
                chunkHeader.forEachRemaining(record -> {System.out.println(record);});
            });
        }
    }
}
