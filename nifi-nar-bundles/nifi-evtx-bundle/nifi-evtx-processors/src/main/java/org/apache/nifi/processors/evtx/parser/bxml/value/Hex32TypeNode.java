package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class Hex32TypeNode extends VariantTypeNode {
    private final String value;

    public Hex32TypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        value = "0x" + binaryReader.readDWord().toString(16);
    }

    @Override
    public String getValue() {
        return value;
    }
}
