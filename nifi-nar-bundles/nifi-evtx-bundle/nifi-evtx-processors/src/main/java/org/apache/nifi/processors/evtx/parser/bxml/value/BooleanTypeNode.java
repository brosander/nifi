package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class BooleanTypeNode extends VariantTypeNode {
    private final boolean value;

    public BooleanTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        UnsignedInteger unsignedInteger = binaryReader.readDWord();
        value = unsignedInteger.intValue() > 0;
    }

    @Override
    public String getValue() {
        return Boolean.toString(value);
    }
}
