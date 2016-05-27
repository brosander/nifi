package org.apache.nifi.processors.evtx.parser.bxml.value;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/**
 * Created by brosander on 5/26/16.
 */
public class FiletimeTypeNode extends VariantTypeNode {
    private final Date value;

    public FiletimeTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        value = readFileTime();
    }

    @Override
    public String getValue() {
        return value.toString();
    }
}
