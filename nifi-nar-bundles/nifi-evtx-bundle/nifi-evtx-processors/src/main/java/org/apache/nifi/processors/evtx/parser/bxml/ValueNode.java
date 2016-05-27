package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.value.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by brosander on 5/25/16.
 */
public class ValueNode extends BxmlNodeWithToken {
    /*public static final VariantTypeNodeFactory[] factories = new VariantTypeNodeFactory[]{NullTypeNode::new,
            WStringTypeNode::new, StringTypeNode::new, SignedByteTypeNode::new, UnsignedByteTypeNode::new,
            SignedWordTypeNode::new, UnsignedWordTypeNode::new, SignedDWordTypeNode::new, UnsignedDWordTypeNode::new,
            SignedQWordTypeNode::new, UnsignedQWordTypeNode::new, FloatTypeNode::new, DoubleTypeNode::new,
            BooleanTypeNode::new, BinaryTypeNode::new, GuidTypeNode::new, SizeTypeNode::new, FiletimeTypeNode::new,
            SystemtimeTypeNode::new, SIDTypeNode::new, Hex32TypeNode::new, Hex64TypeNode::new, BXmlTypeNode::new,
            WStringArrayTypeNode::new};*/

    public static final Map<Integer, VariantTypeNodeFactory> factories = initFactories();

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
        VariantTypeNode variantTypeNode = factories.get(type).create(getInputStream(), getCurrentOffset(), getChunkHeader(), this, -1);
        return Collections.singletonList(variantTypeNode);
    }
}
