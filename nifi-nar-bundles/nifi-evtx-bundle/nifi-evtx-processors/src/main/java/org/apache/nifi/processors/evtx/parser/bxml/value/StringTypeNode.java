package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class StringTypeNode extends VariantTypeNode {
    private final String value;

    public StringTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        if (length >= 0) {
            value = binaryReader.readString(length);
        } else {
            value = binaryReader.readString(binaryReader.readWord());
        }
    }

    @Override
    public String getValue() {
        return value;
    }
}
