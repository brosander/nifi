package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;
import org.apache.nifi.processors.evtx.parser.bxml.RootNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class BXmlTypeNode extends VariantTypeNode {
    private final RootNode rootNode;

    public BXmlTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        rootNode = new RootNode(binaryReader, chunkHeader, this);
    }

    public RootNode getRootNode() {
        return rootNode;
    }

    @Override
    public String getValue() {
        return rootNode.toString();
    }
}
