package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;
import org.apache.nifi.processors.evtx.parser.NumberUtil;

import java.io.IOException;

/**
 * Created by brosander on 5/25/16.
 */
public class TemplateNode extends BxmlNode {
    private final int nextOffset;
    private final UnsignedInteger templateId;
    private final String guid;
    private final int dataLength;

    public TemplateNode(BinaryReader binaryReader, ChunkHeader chunkHeader) throws IOException {
        super(binaryReader, chunkHeader, null);
        nextOffset = NumberUtil.intValueMax(binaryReader.readDWord(), Integer.MAX_VALUE, "Invalid offset.");

        //TemplateId and Guid overlap
        templateId = new BinaryReader(binaryReader, binaryReader.getPosition()).readDWord();
        guid = binaryReader.readGuid();
        dataLength = NumberUtil.intValueMax(binaryReader.readDWord(), Integer.MAX_VALUE - 0x18, "Data length too large.");
        init();
    }

    @Override
    public String toString() {
        return "TemplateNode{" +
                "nextOffset=" + nextOffset +
                ", templateId=" + templateId +
                ", guid='" + guid + '\'' +
                ", dataLength=" + dataLength +
                '}';
    }

    public int getNextOffset() {
        return nextOffset;
    }

    public UnsignedInteger getTemplateId() {
        return templateId;
    }

    public String getGuid() {
        return guid;
    }

    public int getDataLength() {
        return dataLength;
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
