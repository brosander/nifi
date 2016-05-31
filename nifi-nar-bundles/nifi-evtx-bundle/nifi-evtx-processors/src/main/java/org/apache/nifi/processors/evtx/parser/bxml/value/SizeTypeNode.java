package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class SizeTypeNode extends VariantTypeNode {
    private final Number value;

    public SizeTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        if (length == 4) {
            value = binaryReader.readDWord();
        } else {
            value = binaryReader.readQWord();
        }
    }

    @Override
    public String getValue() {
        return value.toString();
    }
}
