package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;

import java.io.IOException;
import java.util.Date;

/**
 * Created by brosander on 5/25/16.
 */
public class Record extends Block {
    private final int magicNumber;
    private final int size;
    private final UnsignedLong recordNum;
    private final Date timestamp;
    private final RootNode rootNode;
    private final int size2;

    public Record(BinaryReader binaryReader, ChunkHeader chunkHeader) throws IOException {
        super(binaryReader, chunkHeader.getOffset() + binaryReader.getPosition());
        magicNumber = NumberUtil.intValueExpected(binaryReader.readDWord(), 10794, "Invalid magic number.");
        size = NumberUtil.intValueMax(binaryReader.readDWord(), 0x10000, "Invalid size.");
        recordNum = binaryReader.readQWord();
        timestamp = binaryReader.readFileTime();
        rootNode = new RootNode(binaryReader, chunkHeader, null);
        int desiredPosition = getInitialPosition() + size - 4;
        int skipAmount = desiredPosition - binaryReader.getPosition();
        if (skipAmount > 0) {
            binaryReader.skip(skipAmount);
        }
        size2 = NumberUtil.intValueExpected(binaryReader.readDWord(), size, "Size 2 doesn't match.");
    }

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

    public UnsignedLong getRecordNum() {
        return recordNum;
    }

    public RootNode getRootNode() {
        return rootNode;
    }
}
