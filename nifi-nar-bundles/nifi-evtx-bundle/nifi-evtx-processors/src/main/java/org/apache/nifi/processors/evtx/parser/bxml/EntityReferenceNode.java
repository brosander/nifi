package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class EntityReferenceNode extends BxmlNodeWithTokenAndString {
    public EntityReferenceNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        init();
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }

    public String getValue() {
        return "&" + getStringValue() + ";";
    }
}
