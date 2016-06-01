package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.NumberUtil;

import java.io.IOException;

/**
 * Created by brosander on 5/25/16.
 */
public class OpenStartElementNode extends BxmlNodeWithToken {
    private final int unknown;
    private final UnsignedInteger size;
    private final int stringOffset;
    private final String tagName;
    private final int tagLength;

    public OpenStartElementNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        if ((getFlags() & 0x0b) != 0) {
            throw new IOException("Invalid flag detected");
        }
        unknown = binaryReader.readWord();
        size = binaryReader.readDWord();
        stringOffset = NumberUtil.intValueMax(binaryReader.readDWord(), Integer.MAX_VALUE, "Invalid string offset.");
        int tagLength = 11;
        if ((getFlags() & 0x04) > 0) {
            tagLength += 4;
        }
        String string = getChunkHeader().getString(stringOffset);
        if (stringOffset > getOffset() - chunkHeader.getOffset()) {
            int initialPosition = binaryReader.getPosition();
            NameStringNode nameStringNode = chunkHeader.addNameStringNode(stringOffset, binaryReader);
            tagLength += binaryReader.getPosition() - initialPosition;
            tagName = nameStringNode.getString();
        } else {
            tagName = string;
        }
        this.tagLength = tagLength;
        init();
    }

    public String getTagName() {
        return tagName;
    }

    @Override
    protected int getHeaderLength() {
        return tagLength;
    }

    @Override
    protected int[] getEndTokens() {
        return new int[]{CLOSE_EMPTY_ELEMENT_TOKEN, CLOSE_ELEMENT_TOKEN};
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
