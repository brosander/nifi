package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class UnsignedByteTypeNode extends VariantTypeNode {
    private int value;

    public UnsignedByteTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        value = binaryReader.read();
    }

    @Override
    public String getValue() {
        return "" + value;
    }
}
