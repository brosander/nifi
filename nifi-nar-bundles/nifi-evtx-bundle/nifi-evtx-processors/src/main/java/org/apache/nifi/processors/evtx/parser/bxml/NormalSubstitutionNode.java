package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class NormalSubstitutionNode extends BxmlNodeWithToken {
    private final UnsignedInteger index;
    private final int type;

    public NormalSubstitutionNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        index = binaryReader.readWord();
        type = binaryReader.read();
        if (getFlags() != 0) {
            throw new IOException("Invalid flags");
        }
        if (getToken() != NORMAL_SUBSTITUTION_TOKEN) {
            throw new IOException("Invalid token");
        }
        init();
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }

    public UnsignedInteger getIndex() {
        return index;
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
