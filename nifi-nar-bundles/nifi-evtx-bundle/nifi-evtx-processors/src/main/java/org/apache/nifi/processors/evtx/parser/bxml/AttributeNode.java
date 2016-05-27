package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/25/16.
 */
public class AttributeNode extends BxmlNodeWithTokenAndString {
    public AttributeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        init();
    }

    @Override
    protected int getMaxChildren() {
        return 1;
    }

    public String getAttributeName() {
        return getStringValue();
    }

    public BxmlNode getValue() {
        return getChildren().get(0);
    }
}
