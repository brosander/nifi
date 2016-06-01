package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.NumberUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class StreamStartNode extends BxmlNodeWithToken {
    private final int unknown;
    private final int unknown2;

    public StreamStartNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        if (getFlags() != 0) {
            throw new IOException("Invalid flags");
        }
        if (getToken() != START_OF_STREAM_TOKEN) {
            throw new IOException("Invalid token " + getToken());
        }
        unknown = NumberUtil.intValueExpected(binaryReader.read(), 1, "Unexpected value for unknown field.");
        unknown2 = NumberUtil.intValueExpected(binaryReader.readWord(), 1, "Unexpected value for unknown field 2");
        init();
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
