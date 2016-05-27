package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class EndOfStreamNode extends BxmlNode {
    public EndOfStreamNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        init();
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }
}
