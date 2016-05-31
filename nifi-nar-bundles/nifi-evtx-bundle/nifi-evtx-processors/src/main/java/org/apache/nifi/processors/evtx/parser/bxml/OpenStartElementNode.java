package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/25/16.
 */
public class OpenStartElementNode extends BxmlNodeWithToken {
    private final UnsignedInteger unknown;
    private final UnsignedInteger size;
    private final UnsignedInteger stringOffset;
    private final String tagName;
    private final int tagLength;

    public OpenStartElementNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        if ((getFlags() & 0x0b) != 0) {
            throw new IOException("Invalid flag detected");
        }
        unknown = readWord();
        size = readDWord();
        stringOffset = readDWord();
        int tagLength = 11;
        if ((getFlags() & 0x04) > 0) {
            tagLength += 4;
        }
        String string = getChunkHeader().getString(stringOffset);
        if (stringOffset.compareTo(UnsignedInteger.valueOf(offset - chunkHeader.getOffset())) > 0) {
            NameStringNode nameStringNode = chunkHeader.addNameStringNode(stringOffset, getInputStream());
            tagLength += nameStringNode.getImplicitOffset();
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
    protected int getEndOfHeader() {
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
