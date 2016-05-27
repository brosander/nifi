package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class SizeTypeNode extends VariantTypeNode {
    private final Number value;

    public SizeTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        if (length == 4) {
            value = readDWord();
        } else {
            value = readQWord();
        }
    }

    @Override
    public String getValue() {
        return value.toString();
    }
}
