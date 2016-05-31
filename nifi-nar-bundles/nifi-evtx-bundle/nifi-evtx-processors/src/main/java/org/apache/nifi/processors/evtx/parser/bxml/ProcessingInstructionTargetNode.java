package org.apache.nifi.processors.evtx.parser.bxml;

import com.google.common.primitives.UnsignedInteger;
import org.apache.nifi.processors.evtx.parser.BxmlNodeVisitor;
import org.apache.nifi.processors.evtx.parser.ChunkHeader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

/**
 * Created by brosander on 5/25/16.
 */
public class ProcessingInstructionTargetNode extends BxmlNodeWithToken {

    private final UnsignedInteger stringLength;
    private final String instruction;
    private final int tagLength;

    public ProcessingInstructionTargetNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        stringLength = readWord();
        if (stringLength.compareTo(UnsignedInteger.ZERO) > 0) {
            instruction = readWString(stringLength.intValue());
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
