package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class BooleanTypeNode extends VariantTypeNode {
    private final boolean value;

    public BooleanTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        UnsignedInteger unsignedInteger = readDWord();
        value = unsignedInteger.intValue() > 0;
    }

    @Override
    public String getValue() {
        return Boolean.toString(value);
    }
}
