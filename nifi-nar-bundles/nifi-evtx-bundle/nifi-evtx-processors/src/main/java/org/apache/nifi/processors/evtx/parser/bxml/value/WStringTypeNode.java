package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class WStringTypeNode extends VariantTypeNode {
    private final String value;

    public WStringTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        if (length >= 0) {
            value = readWString(length / 2);
        } else {
            int characters = readWord().intValue();
            value = readWString(characters);
        }
    }

    @Override
    public String getValue() {
        return value;
    }
}
