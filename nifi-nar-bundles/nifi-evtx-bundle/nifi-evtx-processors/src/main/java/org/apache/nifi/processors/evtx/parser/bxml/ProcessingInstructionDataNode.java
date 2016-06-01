package org.apache.nifi.processors.evtx.parser.bxml;

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
public class ProcessingInstructionDataNode extends BxmlNodeWithToken {
    private final int stringLength;
    private final int tagLength;
    private final String data;

    public ProcessingInstructionDataNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        stringLength = NumberUtil.intValueMax(binaryReader.readDWord(), Integer.MAX_VALUE, "Invalid string length.");
        tagLength = 3 + (2 * stringLength);
        if (stringLength > 0) {
            data = binaryReader.readWString(stringLength);
        } else {
            data = "";
        }
        init();
    }

    public String getValue() {
        return data + "?>";
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }
}
