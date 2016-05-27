package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by brosander on 5/25/16.
 */
public class Record extends Block {
    private final UnsignedInteger magicNumber;
    private final UnsignedInteger size;
    private final UnsignedLong recordNum;
    private final Date timestamp;
    private final RootNode rootNode;
    private final UnsignedInteger size2;

    @Override
    public String toString() {
        return "Record{" +
                "magicNumber=" + magicNumber +
                ", size=" + size +
                ", recordNum=" + recordNum +
                ", timestamp=" + timestamp +
                ", rootNode=" + rootNode +
                ", size2=" + size2 +
                '}';
    }

    public Record(InputStream inputStream, long offset, ChunkHeader chunkHeader) throws IOException {
        super(inputStream, offset);
        magicNumber = readDWord();
        if (magicNumber.intValue() != 10794) {
            throw new IOException("Invalid magic number");
        }
        size = readDWord();
        if (size.compareTo(UnsignedInteger.valueOf(Integer.MAX_VALUE)) > 0 || size.intValue() > 0x10000) {
            throw new IOException("Invalid size");
        }
        recordNum = readQWord();
        timestamp = readFileTime();
        long currentOffset = getCurrentOffset();
        rootNode = new RootNode(new ByteArrayInputStream(readBytes(size.intValue() - 28)), currentOffset, chunkHeader, null);
        size2 = readDWord();
    }

    public UnsignedLong getRecordNum() {
        return recordNum;
    }
}
