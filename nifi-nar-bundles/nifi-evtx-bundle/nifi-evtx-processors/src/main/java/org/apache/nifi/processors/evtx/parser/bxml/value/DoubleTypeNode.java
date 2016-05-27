package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class DoubleTypeNode extends VariantTypeNode {
    private final double value;

    public DoubleTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        UnsignedLong unsignedLong = readQWord();
        value = Double.longBitsToDouble(unsignedLong.longValue());
    }

    @Override
    public String getValue() {
        return Double.toString(value);
    }
}
