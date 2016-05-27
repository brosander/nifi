package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class BinaryTypeNode extends VariantTypeNode {
    private final String value;

    public BinaryTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        if (length >= 0) {
            value = readAndBase64EncodeBinary(length);
        } else {
            value = readAndBase64EncodeBinary(readDWord().intValue());
        }
    }

    @Override
    public String getValue() {
        return value;
    }
}
