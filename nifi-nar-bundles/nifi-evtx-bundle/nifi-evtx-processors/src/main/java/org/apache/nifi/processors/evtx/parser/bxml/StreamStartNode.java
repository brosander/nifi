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
public class StreamStartNode extends BxmlNodeWithToken {
    private final int unknown;
    private final UnsignedInteger unknown2;

    public StreamStartNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        if (getFlags() != 0) {
            throw new IOException("Invalid flags");
        }
        if (getToken() != START_OF_STREAM_TOKEN) {
            throw new IOException("Invalid token " + getToken());
        }
        unknown = binaryReader.read();
        if (unknown != 1) {
            throw new IOException("Unexpected value for unknown field");
        }
        unknown2 = binaryReader.readWord();
        if (unknown2.intValue() != 1) {
            throw new IOException("Unexpected value for unknown field 2");
        }
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
