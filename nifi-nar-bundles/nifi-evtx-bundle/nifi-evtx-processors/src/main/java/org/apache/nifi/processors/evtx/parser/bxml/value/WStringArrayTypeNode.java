package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class WStringArrayTypeNode extends VariantTypeNode {
    private final String value;

    public WStringArrayTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        if (length >= 0) {
            value = readString(length);
        } else {
            UnsignedInteger binaryLength = readWord();
            value = readString(binaryLength.intValue());
        }
    }

    @Override
    public String getValue() {
        return "TODO: " + value;
    }
}
