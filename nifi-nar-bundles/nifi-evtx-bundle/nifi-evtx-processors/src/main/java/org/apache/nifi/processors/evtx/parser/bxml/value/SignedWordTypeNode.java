package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class SignedWordTypeNode extends VariantTypeNode {
    private final int value;

    public SignedWordTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        value = binaryReader.readWord();
    }

    @Override
    public String getValue() {
        return Short.toString((short) value);
    }
}
