package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BinaryReader;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class ProcessingInstructionTargetNode extends BxmlNodeWithToken {

    private final UnsignedInteger stringLength;
    private final String instruction;
    private final int tagLength;

    public ProcessingInstructionTargetNode(BinaryReader binaryReader, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(binaryReader, chunkHeader, parent);
        stringLength = binaryReader.readWord();
        if (stringLength.compareTo(UnsignedInteger.ZERO) > 0) {
            instruction = binaryReader.readWString(stringLength.intValue());
        } else {
            instruction = "";
        }
        tagLength = 3 + (2 * stringLength.intValue());
        init();
    }

    @Override
    protected List<BxmlNode> initChildren() throws IOException {
        return Collections.emptyList();
    }

    @Override
    public void accept(BxmlNodeVisitor bxmlNodeVisitor) throws IOException {
        bxmlNodeVisitor.visit(this);
    }

    public String getValue() {
        return instruction + "?>";
    }
}
