package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by brosander on 5/25/16.
 */
public class TemplateNode extends BxmlNode {
    private final UnsignedInteger nextOffset;
    private final UnsignedInteger templateId;
    private final String guid;
    private final UnsignedInteger dataLength;

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

    public TemplateNode(InputStream inputStream, long offset, ChunkHeader chunkHeader) throws IOException {
        super(inputStream, offset, chunkHeader, null);
        nextOffset = readDWord();
        byte[] bytes = readBytes(16);
        templateId = new BinaryReader(new ByteArrayInputStream(bytes)).readDWord();
        guid = new BinaryReader(new ByteArrayInputStream(bytes)).readGuid();
        dataLength = readDWord();
        if (dataLength.plus(UnsignedInteger.valueOf(0x18)).compareTo(UnsignedInteger.valueOf(Integer.MAX_VALUE)) > 1) {
            throw new IOException("Data getLength is too large");
        }
        init();
    }
}
