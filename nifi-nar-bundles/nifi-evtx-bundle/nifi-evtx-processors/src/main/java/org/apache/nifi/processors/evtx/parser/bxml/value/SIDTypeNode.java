package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;

/**
 * Created by brosander on 5/26/16.
 */
public class SIDTypeNode extends VariantTypeNode {
    private final String value;

    public SIDTypeNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(binaryReader, chunkHeader, parent, length);
        int version = binaryReader.read();
        int num_elements = binaryReader.read();
        UnsignedInteger id_high = binaryReader.readDWordBE();
        int id_low = binaryReader.readWordBE();
        StringBuilder builder = new StringBuilder("S-");
        builder.append(version);
        builder.append("-");
        builder.append((id_high.longValue() << 16) ^ id_low);
        for (int i = 0; i < num_elements; i++) {
            builder.append("-");
            builder.append(binaryReader.readDWord());
        }
        value = builder.toString();
    }

    @Override
    public String getValue() {
        return value;
    }
}
