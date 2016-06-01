package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.NumberUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class TemplateInstanceNode extends BxmlNodeWithToken {

    private final int unknown;
    private final UnsignedInteger templateId;
    private final int templateOffset;
    private final boolean isResident;
    private final TemplateNode templateNode;
    private final int templateLength;

    public TemplateInstanceNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        unknown = binaryReader.read();
        templateId = binaryReader.readDWord();
        templateOffset = NumberUtil.intValueMax(binaryReader.readDWord(), Integer.MAX_VALUE, "Invalid template offset.");
        if (templateOffset > getOffset() - chunkHeader.getOffset()) {
            isResident = true;
            templateNode = chunkHeader.addTemplateNode(templateOffset, binaryReader);
            templateLength = 0;
        } else {
            isResident = false;
            int initialPosition = binaryReader.getPosition();
            templateNode = chunkHeader.getTemplateNode(templateOffset);
            templateLength = binaryReader.getPosition() - initialPosition;
        }

        if (templateNode != null && !templateId.equals(templateNode.getTemplateId())) {
            throw new IOException("Invalid template id");
        }
        init();
    }

    @Override
    protected int getHeaderLength() {
        return 10 + templateLength;
    }

    public TemplateNode getTemplateNode() {
        return templateNode;
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public boolean hasEndOfStream() {
        return super.hasEndOfStream() || templateNode.hasEndOfStream();
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
