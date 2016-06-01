package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.value.*;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brosander on 5/25/16.
 */
public class ValueNode extends BxmlNodeWithToken {
    public static final Map<Integer, VariantTypeNodeFactory> factories = initFactories();
    private final int type;

    public ValueNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        if ((getFlags() & 0x0B) != 0) {
            throw new IOException("Invalid flag");
        }
        type = binaryReader.read();
        init();
    }

    private static final Map<Integer, VariantTypeNodeFactory> initFactories() {
        Map<Integer, VariantTypeNodeFactory> result = new HashMap<>();
        result.put(0, NullTypeNode::new);
        result.put(1, WStringTypeNode::new);
        result.put(2, StringTypeNode::new);
        result.put(3, SignedByteTypeNode::new);
        result.put(4, UnsignedByteTypeNode::new);
        result.put(5, SignedWordTypeNode::new);
        result.put(6, UnsignedWordTypeNode::new);
        result.put(7, SignedDWordTypeNode::new);
        result.put(8, UnsignedDWordTypeNode::new);
        result.put(9, SignedQWordTypeNode::new);
        result.put(10, UnsignedQWordTypeNode::new);
        result.put(11, FloatTypeNode::new);
        result.put(12, DoubleTypeNode::new);
        result.put(13, BooleanTypeNode::new);
        result.put(14, BinaryTypeNode::new);
        result.put(15, GuidTypeNode::new);
        result.put(16, SizeTypeNode::new);
        result.put(17, FiletimeTypeNode::new);
        result.put(18, SystemtimeTypeNode::new);
        result.put(19, SIDTypeNode::new);
        result.put(20, Hex32TypeNode::new);
        result.put(21, Hex64TypeNode::new);
        result.put(33, BXmlTypeNode::new);
        result.put(129, WStringArrayTypeNode::new);
        return Collections.unmodifiableMap(result);
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        VariantTypeNode variantTypeNode = factories.get(type).create(getBinaryReader(), getChunkHeader(), this, -1);
        return Collections.singletonList(variantTypeNode);
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
