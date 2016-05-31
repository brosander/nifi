package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedLong;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class DoubleTypeNode extends VariantTypeNode {
    private final double value;

    public DoubleTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        UnsignedLong unsignedLong = binaryReader.readQWord();
        value = Double.longBitsToDouble(unsignedLong.longValue());
    }

    @Override
    public String getValue() {
        return Double.toString(value);
    }
}
