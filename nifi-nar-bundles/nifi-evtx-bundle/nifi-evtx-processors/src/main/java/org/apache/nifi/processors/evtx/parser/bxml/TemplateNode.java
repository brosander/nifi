package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;

/**
 * Created by brosander on 5/25/16.
 */
public class TemplateNode extends BxmlNode {
    private final UnsignedInteger nextOffset;
    private final UnsignedInteger templateId;
    private final String guid;
    private final UnsignedInteger dataLength;

    public TemplateNode(BinaryReader binaryReader, ChunkHeader chunkHeader) throws IOException {
        super(binaryReader, chunkHeader, null);
        nextOffset = binaryReader.readDWord();
        templateId = new BinaryReader(binaryReader, binaryReader.getPosition()).readDWord();
        guid = binaryReader.readGuid();
        dataLength = binaryReader.readDWord();
        if (dataLength.plus(UnsignedInteger.valueOf(0x18)).compareTo(UnsignedInteger.valueOf(Integer.MAX_VALUE)) > 1) {
            throw new IOException("Data getLength is too large");
        }
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

    public UnsignedInteger getNextOffset() {
        return nextOffset;
    }

    public UnsignedInteger getTemplateId() {
        return templateId;
    }

    public String getGuid() {
        return guid;
    }

    public UnsignedInteger getDataLength() {
        return dataLength;
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
