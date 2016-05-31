package org.apache.nifi.processors.evtx.parser.bxml;

import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.Block;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public abstract class BxmlNode extends Block {
    public static final int END_OF_STREAM_TOKEN = 0x00;
    public static final int CLOSE_EMPTY_ELEMENT_TOKEN = 0x03;
    public static final int CLOSE_ELEMENT_TOKEN = 0x04;
    public static final int C_DATA_SECTION_TOKEN = 0x07;
    public static final int NORMAL_SUBSTITUTION_TOKEN = 0x0D;
    public static final int CONDITIONAL_SUBSTITUTION_TOKEN = 0x0E;
    public static final int START_OF_STREAM_TOKEN = 0x0F;

    private static final BxmlNodeFactory[] factories = new BxmlNodeFactory[]{EndOfStreamNode::new, OpenStartElementNode::new,
            CloseStartElementNode::new, CloseEmptyElementNode::new, CloseElementNode::new, ValueNode::new, AttributeNode::new,
            CDataSectionNode::new, null, EntityReferenceNode::new, ProcessingInstructionTargetNode::new,
            ProcessingInstructionDataNode::new, TemplateInstanceNode::new, NormalSubstitutionNode::new,
            ConditionalSubstitutionNode::new, StreamStartNode::new};
    private final ChunkHeader chunkHeader;
    private final BxmlNode parent;
    private boolean hasEndOfStream = false;
    protected List<BxmlNode> children;

    protected BxmlNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) {
        super(binaryReader, chunkHeader.getOffset() + binaryReader.getPosition());
        this.chunkHeader = chunkHeader;
        this.parent = parent;
        hasEndOfStream = false;
    }

    @Override
    protected void init(boolean shouldClearBinaryReader) throws IOException {
        super.init(false);
        children = Collections.unmodifiableList(initChildren());
        if (shouldClearBinaryReader) {
            clearBinaryReader();
        }
    }

    protected List<BxmlNode> initChildren() throws IOException {
        BinaryReader binaryReader = getBinaryReader();
        List<BxmlNode> result = new ArrayList<>();
        int maxChildren = getMaxChildren();
        int[] endTokens = getEndTokens();
        for (int i = 0; i < maxChildren; i++) {
            // Masking flags for location of factory
            int token = binaryReader.peek();
            int factoryIndex = token & 0x0F;
            if (factoryIndex > factories.length - 1) {
                throw new IOException("Invalid token " + factoryIndex);
            }
            BxmlNodeFactory factory = factories[factoryIndex];
            if (factory == null) {
                throw new IOException("Invalid token " + factoryIndex);
            }
            BxmlNode bxmlNode = factory.create(binaryReader, chunkHeader, this);
            result.add(bxmlNode);
            if (bxmlNode.hasEndOfStream() || bxmlNode instanceof EndOfStreamNode) {
                hasEndOfStream = true;
                break;
            }
            if (Arrays.binarySearch(endTokens, factoryIndex) >= 0) {
                break;
            }
        }
        return result;
    }

    protected int getMaxChildren() {
        return Integer.MAX_VALUE;
    }

    protected int[] getEndTokens() {
        return new int[]{END_OF_STREAM_TOKEN};
    }

    public List<BxmlNode> getChildren() {
        if (!isInitialized()) {
            throw new RuntimeException("Need to initialize children");
        }
        return children;
    }

    public ChunkHeader getChunkHeader() {
        return chunkHeader;
    }

    public BxmlNode getParent() {
        return parent;
    }

    public boolean hasEndOfStream() {
        return hasEndOfStream;
    }

    public abstract void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException;
}
