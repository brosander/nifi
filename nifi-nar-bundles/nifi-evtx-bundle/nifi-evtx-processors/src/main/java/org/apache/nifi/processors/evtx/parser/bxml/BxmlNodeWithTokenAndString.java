package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/25/16.
 */
public abstract class BxmlNodeWithTokenAndString extends BxmlNodeWithToken {
    private final UnsignedInteger stringOffset;
    private final String value;
    private final int tagLength;

    public BxmlNodeWithTokenAndString(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        stringOffset = readDWord();
        int tagLength = getBaseTagLength();
        if (stringOffset.compareTo(UnsignedInteger.valueOf(offset - chunkHeader.getOffset())) > 0) {
            NameStringNode nameStringNode = chunkHeader.addNameStringNode(stringOffset, getInputStream());
            tagLength += nameStringNode.getImplicitOffset();
            value = nameStringNode.getString();
        } else {
            value = chunkHeader.getString(stringOffset);
        }
        this.tagLength = tagLength;
    }

    @Override
    protected int getEndOfHeader() {
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
