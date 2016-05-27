package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.bxml.value.VariantTypeNode;
import org.apache.nifi.processors.evtx.parser.bxml.value.VariantTypeNodeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class RootNode extends BxmlNode {
    private final UnsignedInteger substitutionCount;
    private final List<VariantTypeNode> substitutions;

    public RootNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        init();
        substitutionCount = readDWord();
        List<VariantTypeSizeAndFactory> substitutionVariantFactories = new ArrayList<>(substitutionCount.intValue());
        for (long i = 0; i < substitutionCount.longValue(); i++) {
            try {
                UnsignedInteger substitionSize = readWord();
                int substituionType = readWord().intValue();
                substitutionVariantFactories.add(new VariantTypeSizeAndFactory(substitionSize.intValue(), ValueNode.factories.get(substituionType)));
            } catch (Exception e) {
                System.out.println(i);
            }
        }
        List<VariantTypeNode> substitutions = new ArrayList<>();
        for (VariantTypeSizeAndFactory substitutionVariantFactory : substitutionVariantFactories) {
            substitutions.add(substitutionVariantFactory.factory.create(getInputStream(), getCurrentOffset(), chunkHeader, this, substitutionVariantFactory.size));
        }
        this.substitutions = Collections.unmodifiableList(substitutions);
    }

    @Override
    public String toString() {
        return "RootNode{" + getChildren() + "}";
    }

    public class VariantTypeSizeAndFactory {
        private final int size;
        private final VariantTypeNodeFactory factory;

        public VariantTypeSizeAndFactory(int size, VariantTypeNodeFactory factory) {
            this.size = size;
            this.factory = factory;
        }
    }
}
