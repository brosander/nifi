package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class TemplateInstanceNode extends BxmlNodeWithToken {

    private final int unknown;
    private final UnsignedInteger templateId;
    private final UnsignedInteger templateOffset;
    private final boolean isResident;
    private final TemplateNode templateNode;

    public TemplateInstanceNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        unknown = read();
        templateId = readDWord();
        templateOffset = readDWord();
        if (templateOffset.compareTo(UnsignedInteger.valueOf(offset - chunkHeader.getOffset())) > 0) {
            isResident = true;
            templateNode = chunkHeader.addTemplateNode(templateOffset, getInputStream());
        } else {
            isResident = false;
            templateNode = chunkHeader.getTemplateNode(templateOffset);
        }

        if (templateNode != null && !templateId.equals(templateNode.getTemplateId())) {
            throw new IOException("Invalid template id");
        }
        init();
    }

    @Override
    protected int getEndOfHeader() {
        return 10 + (isResident ? (int) templateNode.getImplicitOffset() : 0);
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
}
