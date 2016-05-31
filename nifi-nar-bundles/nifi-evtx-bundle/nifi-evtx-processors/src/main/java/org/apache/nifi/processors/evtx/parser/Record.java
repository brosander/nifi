package org.apache.nifi.processors.evtx.parser;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;

import java.io.IOException;
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

    public Record(BinaryReader binaryReader, ChunkHeader chunkHeader) throws IOException {
        super(binaryReader, chunkHeader.getOffset() + binaryReader.getPosition());
        magicNumber = binaryReader.readDWord();
        if (magicNumber.intValue() != 10794) {
            throw new IOException("Invalid magic number");
        }
        size = binaryReader.readDWord();
        if (size.compareTo(UnsignedInteger.valueOf(Integer.MAX_VALUE)) > 0 || size.intValue() > 0x10000) {
            throw new IOException("Invalid size");
        }
        recordNum = binaryReader.readQWord();
        timestamp = binaryReader.readFileTime();
        rootNode = new RootNode(binaryReader, chunkHeader, null);
        int desiredPosition = getInitialPosition() + size.intValue() - 4;
        int skipAmount = desiredPosition - binaryReader.getPosition();
        if (skipAmount > 0) {
            binaryReader.skip(skipAmount);
        }
        size2 = binaryReader.readDWord();
        if (!size.equals(size2)) {
            throw new IOException("Size 2 invalid");
        }
    }

    public UnsignedLong getRecordNum() {
        return recordNum;
    }

    public RootNode getRootNode() {
        return rootNode;
    }
}
