package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.value.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class ValueNode extends BxmlNodeWithToken {
    private static final VariantTypeNodeFactory[] factories = new VariantTypeNodeFactory[]{NullTypeNode::new,
            WStringTypeNode::new, StringTypeNode::new, SignedByteTypeNode::new, UnsignedByteTypeNode::new,
            SignedWordTypeNode::new, UnsignedWordTypeNode::new, SignedDWordTypeNode::new, UnsignedDWordTypeNode::new,
            SignedQWordTypeNode::new, UnsignedQWordTypeNode::new, FloatTypeNode::new, DoubleTypeNode::new,
            BooleanTypeNode::new, BinaryTypeNode::new, GuidTypeNode::new, SizeTypeNode::new, FiletimeTypeNode::new,
            SystemtimeTypeNode::new, SIDTypeNode::new, Hex32TypeNode::new, Hex64TypeNode::new, BXmlTypeNode::new,
            WStringArrayTypeNode::new};

    private final int type;

    public ValueNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        if ((getFlags() & 0x0B) != 0) {
            throw new IOException("Invalid flag");
        }
        type = read();
        init();
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        VariantTypeNode variantTypeNode = factories[type].create(getInputStream(), getCurrentOffset(), getChunkHeader(), this, -1);
        return Collections.singletonList(variantTypeNode);
    }
}
