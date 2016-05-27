package org.apache.nifi.processors.evtx.parser.bxml.value;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.BxmlNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/26/16.
 */
public class SIDTypeNode extends VariantTypeNode {
    private final String value;

    public SIDTypeNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent, int length) throws IOException {
        super(inputStream, offset, chunkHeader, parent, length);
        int version = read();
        int num_elements = read();
        UnsignedInteger id_high = readDWordBE();
        UnsignedInteger id_low = readWordBE();
        StringBuilder builder = new StringBuilder("S-");
        builder.append(version);
        builder.append("-");
        builder.append((id_high.longValue() << 16) ^ id_low.longValue());
        for (int i = 0; i < num_elements; i++) {
            builder.append("-");
            builder.append(readDWord());
        }
        value = builder.toString();
    }

    @Override
    public String getValue() {
        return value;
    }
}
