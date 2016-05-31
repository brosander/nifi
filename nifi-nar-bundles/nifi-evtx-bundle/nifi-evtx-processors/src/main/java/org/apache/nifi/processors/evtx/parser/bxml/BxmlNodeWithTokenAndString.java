package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;

/**
 * Created by brosander on 5/25/16.
 */
public abstract class BxmlNodeWithTokenAndString extends BxmlNodeWithToken {
    private final UnsignedInteger stringOffset;
    private final String value;
    private final int tagLength;

    public BxmlNodeWithTokenAndString(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        stringOffset = binaryReader.readDWord();
        int tagLength = getBaseTagLength();
        if (stringOffset.compareTo(UnsignedInteger.valueOf(getOffset() - chunkHeader.getOffset())) > 0) {
            int initialPosition = binaryReader.getPosition();
            NameStringNode nameStringNode = chunkHeader.addNameStringNode(stringOffset, binaryReader);
            tagLength += binaryReader.getPosition() - initialPosition;
            value = nameStringNode.getString();
        } else {
            value = chunkHeader.getString(stringOffset);
        }
        this.tagLength = tagLength;
    }

    @Override
    protected int getHeaderLength() {
        return tagLength;
    }

    protected int getBaseTagLength() {
        return 5;
    }

    public UnsignedInteger getStringOffset() {
        return stringOffset;
    }

    public String getStringValue() {
        return value;
    }
}
