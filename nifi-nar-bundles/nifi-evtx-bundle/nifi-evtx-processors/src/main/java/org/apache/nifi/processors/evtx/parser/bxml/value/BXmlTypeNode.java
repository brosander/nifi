package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class BXmlTypeNode extends VariantTypeNode {
    private final RootNode rootNode;

    public BXmlTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        rootNode = new RootNode(getInputStream(), offset, chunkHeader, this);
    }

    @Override
    public String getValue() {
        return rootNode.toString();
    }
}
