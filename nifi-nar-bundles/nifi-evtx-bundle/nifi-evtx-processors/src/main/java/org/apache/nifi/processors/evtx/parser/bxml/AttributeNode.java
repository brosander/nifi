package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;

/**
 * Created by brosander on 5/25/16.
 */
public class AttributeNode extends BxmlNodeWithTokenAndString {
    public AttributeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        init();
    }

    @Override
    protected int getMaxChildren() {
        return 1;
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }

    public String getAttributeName() {
        return getStringValue();
    }

    public BxmlNode getValue() {
        return getChildren().get(0);
    }
}
