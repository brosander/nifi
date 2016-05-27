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
public class ProcessingInstructionDataNode extends BxmlNodeWithToken {

    private final UnsignedInteger stringLength;
    private final int tagLength;
    private final String data;

    public ProcessingInstructionDataNode(InputStream inputStream, long offset, ChunkHeader chunkHeader, BxmlNode parent) throws IOException {
        super(inputStream, offset, chunkHeader, parent);
        stringLength = readDWord();
        tagLength = 3 + (2 * stringLength.intValue());
        if (stringLength.compareTo(UnsignedInteger.ZERO) > 0) {
            data = readWString(stringLength.intValue());
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
}
